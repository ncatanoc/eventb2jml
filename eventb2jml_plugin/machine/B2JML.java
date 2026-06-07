
package eventb2jml_plugin.machine;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eventb.core.EventBPlugin;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCCarrierSet;
import org.eventb.core.ISCConstant;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCGuard;
import org.eventb.core.ISCInternalContext;
import org.eventb.core.ISCInvariant;
import org.eventb.core.ISCParameter;
import org.eventb.core.ISCRefinesEvent;
import org.eventb.core.ISCRefinesMachine;
import org.eventb.core.ISCVariable;
import org.eventb.core.ISCVariant;
import org.eventb.core.ISCWitness;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.ui.eventbeditor.IEventBEditor;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;



import eventb2jml_plugin.MachineUtils;

public class B2JML implements IEditorActionDelegate{
	IEventBEditor<?> editor;
	JButton openButton, saveButton;
	JTextArea log;
	JFileChooser fc;
	Pred2JML Pred2JML;
	MachineUtils utils = new MachineUtils();

	ArrayList<String> vars;

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		if (targetEditor instanceof IEventBEditor)
			editor = (IEventBEditor<?>) targetEditor;
	}

	public void run(IAction action) {
		//System.out.println("Start!!");
		final IRodinFile rodinFile = getSelectedComponent();

		String machineName = rodinFile.getBareName().toString();

		Pred2JML = new Pred2JML();

		//Machine info
		try {
			String output = "";
			ISCRefinesMachine[] refinedMachines = machineRefined(rodinFile,machineName);
			// Determines if it's an abstract or concrete machine
			if (refinedMachines.length == 0) //It's an abstract machine
				output = ruleM(rodinFile,machineName);
			else // It's a concrete machine
				output = ruleN(rodinFile,machineName);
			try {
				System.out.println("import eventb2jml_plugin.models.JML.*;");
				System.out.println("import org.jmlspecs.models.JMLEqualsEqualsPair;");

				//System.out.println("import org.jmlspecs.models.JMLEqualsEqualsPair; \n\n");
				System.out.println(output);
				System.out.println(machineName + ".java");
				String filePath = rodinFile.getResource().getRawLocation().toString();
				filePath = filePath.replace(machineName + "." + rodinFile.getResource().getFileExtension(), "");
				String fileName = machineName + ".java";
				//Create a directory
				String strDirectoy =filePath+"generated_jml";
				boolean success = (
						new File(strDirectoy)).mkdir();
				//System.out.println(strDirectoy);
				//System.out.println(filePath);

				FileWriter fstream = new FileWriter(strDirectoy+File.separator+fileName);
				BufferedWriter out = new BufferedWriter(fstream);
				System.out.println("import eventb2jml_plugin.models.JML.*;");
				//System.out.println("import org.jmlspecs.models.JMLEqualsEqualsPair; \n\n");
				out.write("//version 3.2 \n\nimport eventb2jml_plugin.models.JML.*;\n" +
						"import org.jmlspecs.models.JMLEqualsEqualsPair;\n\n");
				//out.write("import org.jmlspecs.models.JMLEqualsEqualsPair; \n\n");
				out.write(output);
				out.close();
				fstream.close();
				System.out.println("Save in: " + strDirectoy+File.separator+fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		//System.out.println("End!!");
	}

	// Returns the concrete machine 'machineName' in eventB to JML
	public String ruleN(IRodinFile rodinFile, String machineName) throws RodinDBException {
		String res = "";		
		PrintTrace("Starting the translation");
		PrintTrace("1. Sees");
		String sees = Sees(rodinFile, machineName);
		PrintTrace("2. Variables");
		String variables = machineVariables(rodinFile, machineName);
		//Pred2JML.PrintVarT2(Pred2JML.varType);
		PrintTrace("3. Invariants");
		String invariants = machineInvariants(rodinFile, machineName);
		PrintTrace("4. Events");
		String events = events(rodinFile, machineName);

		res +=
				"public abstract class " + machineName + "{\n" +
						sees + "\n" +
						variables + "\n" +
						invariants + "\n" +
						events + "\n" +
						"}";

		//res += invariants;
		return res;
	}

	// Returns the abstract machine 'machineName' in eventB to JML
	public String ruleM(IRodinFile rodinFile, String machineName) throws RodinDBException {
		String res = "";
		PrintTrace("Starting the translation");
		PrintTrace("1. Sees");
		String sees = Sees(rodinFile, machineName);

		// machineVariables method gets all variables name, the type
		// is translated using the type provided by EventB.
		PrintTrace("2. Variables");
		String variables = machineVariables(rodinFile, machineName);
		//Pred2JML.PrintVarT2(Pred2JML.varType);
		PrintTrace("3. Invariants");
		String invariants = machineInvariants(rodinFile, machineName);
		PrintTrace("4. Events");
		String events = events(rodinFile, machineName);

		res +=
				"public abstract class " + machineName + "{\n" +
						sees + "\n" +
						variables + "\n" +
						invariants + "\n" +
						events + "\n" +
						"}";

		//res = variables;
		return res;
	}



	// Returns all machine 'machineName' 's specifications in EventB to JML
	public String events(IRodinFile rodinFile, String machineName) throws RodinDBException {
		StringBuffer res = new StringBuffer("");
		ISCEvent[] evts = utils.fetchmachineEvents(rodinFile, machineName);
		for (ISCEvent evt : evts){
			PrintTrace("Event: " + evt.getLabel());
			if (evt.getLabel().equals("INITIALISATION")){
				res.append(initRule(evt));
			}else{
				HashMap<String,String> par = eventParameters(evt);
				if (par.size() == 0){
					res.append(ruleWhen(evt));
				}else{
					res.append(ruleAny(evt, par));
				}
			}
		}
		return res.toString();
	}

	public String initRule(ISCEvent initEvt) throws RodinDBException{
		ISCAction[] evtActions =  utils.fetchEvtActions(initEvt);
		int num_actions = evtActions.length;
		if (num_actions == 0){
			return "";
		}
		StringBuffer res = new StringBuffer("/*@ initially \n");
		for (ISCAction evtAction : evtActions){
			//translationTrace(evtAction.getLabel(),"act1");
			HashMap<String, ArrayList<String>> tmp = transAssig(evtAction.getAssignmentString());
			int i2 = tmp.size();

			for (String var : tmp.keySet()){
				if (tmp.get(var).get(0).equals("EMPTY")){
					res.append("\t" + var + ".isEmpty()");
				}else if (tmp.get(var).get(0).equals("SET")){
					res.append("\t" + var + ".equals(" + tmp.get(var).get(1) + ")");
				}else if (tmp.get(var).get(0).equals("NATIVE")){
					res.append("\t" + var + " == " + tmp.get(var).get(1));
				}else if (tmp.get(var).get(0).equals("BECOMES")){
					String tt = getInternalType(var);
					res.append(tmp.get(var).get(1));
					/*if (tt.equals(jmlType.intT) || tt.equals(jmlType.boolT)){
						res.append("\t (\\exists " + getJmlType(var) + " " + var + "_prime; " + tmp.get(var).get(1) + 
								".has(" + var + "_prime); " + var + " == " + var + "_prime)");
					}else if (tt.equals(jmlType.setT) || tt.equals(jmlType.relT)){
						res.append("\t (\\exists " + getJmlType(var) + " " + var + "_prime; " + 
								var + "_prime.isSubset(" + tmp.get(var).get(1) + "); " + var + ".equals(" + var + "_prime))");
					} */
				}else{
					res.append("\t" + var + " == " + tmp.get(var).get(1));
				}
				if (i2 > 1){
					res.append(" &&\n");
				}i2--;
			}
			if (num_actions > 1){
				res.append(" &&\n");
			}num_actions--;
		}
		res.append("; @*/ \n\n");
		return res.toString();
	}

	public String ruleWhen(ISCEvent evt) throws RodinDBException{
		String res = "";
		PrintTrace("\t Guards");
		String G = eventGuards(evt);
		PrintTrace("\t Actions");
		String S = eventActions(evt);
		String evtName = evt.getLabel();

		if (G.equals("")){
			G = "true";
		}if (S.equals("")){
			S = "true";
		}

		res += "/*@\t assignable \\nothing;\n";

		res += "\t ensures \\result <==> " + G + "; @*/\n";
		res += "public abstract boolean guard_" + evtName + "();\n\n";

		//Variable vars is filled up when the method eventActions is called.
		//res += "/*@\t assignable ";
		String assig = "";
		int i = vars.size();
		if (i == 0)
			assig = "\\nothing";
		else
			for (String v : vars){
				assig += v;
				if (i > 1){
					assig += ", ";
				}i--;
			}

		res += "/*@ requires guard_" + evtName + "();";
		res += "\n\t assignable " + assig;
		res += ";\n\t ensures \\old(" + G + ") && " + S + "; \n";
		res += "also";
		res += "\n\t requires !guard_" + evtName + "();\n";
		res += "\t assignable \\nothing;";
		res += "\n\t ensures true; @*/\n";
		res += "public abstract void run_" + evtName + "();\n\n";

		return res;
	}

	public String ruleAny(ISCEvent evt, HashMap<String,String> parameters) throws RodinDBException{
		String res="";
		String evtName = evt.getLabel();
		PrintTrace("\t Guards");
		String G = eventGuards(evt);
		PrintTrace("\t Actions");
		String S = eventActions(evt);

		if (G.equals("")){
			G = "true";
		}if (S.equals("")){
			S = "true";
		}
		PrintTrace("\t Parameters");
		String anyParameters = "";
		String par = "";
		int ii = parameters.size();
		for (String p : parameters.keySet()){
			anyParameters += "(\\exists " + parameters.get(p) + " " + p;
			if (ii > 1){
				anyParameters += "; ";
			}ii--;
			par += ")";
		}

		res += "/*@\t assignable \\nothing;\n";
		res += "\t ensures \\result <==> "+ anyParameters + "; " + G + par + "; @*/\n";

		res += "public abstract boolean guard_" + evtName + "();\n\n";

		//Variable vars is filled up when the method eventActions is called.
		//res += "/*@\t assignable ";
		String assig = "";
		int i = vars.size();
		if (i == 0)
			assig = "\\nothing";
		else
			for (String v : vars){
				assig += v;
				if (i > 1){
					assig += ", ";
				}i--;
			}

		res += "/*@ requires guard_" + evtName + "();\n";
		res += "\t assignable " + assig;
		res += ";\n\t ensures " + anyParameters + "; \\old(" + G + ") && " + S + par + ";\n";
		res += "also";
		res += "\n\t requires !guard_" + evtName + "();\n";
		res += "\t assignable \\nothing;";
		res += "\n\t ensures true; @*/\n";

		res += "public abstract void run_" + evtName + "();\n\n";
		return res;
	}

	public String ruleNeg(String predG){
		return "!" + predG;
	}

	public String ruleSkip(){
		return "true";
	}

	public String ruleAsg(String var, String assignment){
		String res = "";
		res += var + ".equals(\\old(" + assignment + "))";
		return res;
	}

	public String getInternalType(String varName){
		return Pred2JML.getInternalType(varName);
	}

	public String getJmlType(String varName){
		return Pred2JML.getJmlType(varName);
	}

	public String transPred(String pred){
		return Pred2JML.Pre(pred);
	}

	public void transExp(String exp){
		Pred2JML.Exp(exp);
	}

	public HashMap<String, ArrayList<String>> transAssig(String assig){
		return Pred2JML.Assignment(assig);
	}

	public String getType(String varName, String varDef){
		return Pred2JML.getvariableType(varName, varDef,true);
	}

	/*** Contexts seen***/

	public String ConstType(ISCConstant constant) throws RodinDBException {
		final FormulaFactory f = FormulaFactory.getDefault();
		//TODO: is missing when the type is a carry set

		String constantType = getType(constant.getElementName(), constant.getType(f).toString());

		return constantType;
	}

	// it transforms a constant declaration in B into JML
	// --> static final T c; where c is the constant and T its type.
	public String Constant(ISCConstant constant) throws RodinDBException {
		String constantName = constant.getIdentifierString();
		String constantType =  ConstType(constant);		
		//String res = "/*@ public static final ghost " + constantType + " " + constantName + "; @*/";
		String res = "/*@ public model " + constantType + " " + constantName + ";\n " +
				"public constraint " + constantName + ".equals(\\old(" + constantName + ")); */";

		return res +"\n";
	}

	// it transforms a list of constant declarations in B into JML
	public String seeConstants(ISCConstant[] constants) throws RodinDBException {
		StringBuffer res = new StringBuffer("");

		for(ISCConstant constant: constants)
			res.append(Constant(constant));

		return res.toString();
	}

	// it transforms an axiom in B into JML
	public String Axiom(ISCAxiom axiom, boolean theorem) throws RodinDBException {
		String res = "";
		if (!theorem){
			//translationTrace(axiom.getLabel(), "ax71");
			res = "/*@ public static invariant "+ transPred(axiom.getPredicateString())+"; @*/";
		}else
			res = "/*@ public static invariant_redundantly "+ transPred(axiom.getPredicateString())+"; @*/";
		return res +"\n";
	}

	// it transforms a list of axioms in B into JML
	public String seeAxioms(ISCAxiom[] axioms) throws RodinDBException {
		StringBuffer res = new StringBuffer("");

		for(ISCAxiom axiom : axioms){
			res.append(Axiom(axiom,axiom.isTheorem()));
		}
		return res.toString();
	}





	// it transforms a carrier set definition in B into JML
	// model JMLEqualsSet<Integer> s; where s is the set
	public String Set(ISCCarrierSet set) throws RodinDBException {
		String resvar = set.getElementName(); 
		//TODO: According with the type the translation changes
		//	See: Set and Enum rule definitions
		//TODO: How to specify that using BSet?
		//String restype = SetType(set);

		Pred2JML.setVarTypeSet(resvar);
		return "/*@ public model BSet<Integer> " + resvar +";" +
		"\n public constraint " + resvar + ".equals(\\old(" + resvar + ")); */\n";
	}

	// it transforms a list of carrier set declarations in B into JML
	public String seeSets(ISCCarrierSet[] sets) throws RodinDBException {
		StringBuffer res = new StringBuffer("");

		for(ISCCarrierSet set: sets)
			res.append(Set(set));

		return res.toString();
	}

	// it transforms a machine context in B into JML
	public String See(ISCInternalContext see) throws RodinDBException {
		// sets
		ISCCarrierSet[] sets = see.getChildrenOfType(ISCCarrierSet.ELEMENT_TYPE);
		// constants
		ISCConstant[] constants = see.getChildrenOfType(ISCConstant.ELEMENT_TYPE);
		// axioms
		ISCAxiom[] axioms = see.getChildrenOfType(ISCAxiom.ELEMENT_TYPE);


		return    seeSets(sets) 
				+ seeConstants(constants)
				+ seeAxioms(axioms);
	}

	// it transforms a list of contexts in B into JML
	public String Sees(IRodinFile rodinFile, String machineName) throws RodinDBException {
		ISCInternalContext[] sees = utils.fetchMachineContexts(rodinFile,machineName);

		StringBuffer res = new StringBuffer("");
		for(ISCInternalContext see : sees)
			res.append(See(see));

		return res.toString();
	}


	// Returns machine 'machineName' refined machines in EventB to JML
	public ISCRefinesMachine[] machineRefined(IRodinFile rodinFile, String machineName) throws RodinDBException {
		ISCRefinesMachine[] refinedMachines =  utils.fetchRefinedMachines(rodinFile, machineName);
		return refinedMachines;
	}

	// Returns machine 'machineName' invariants (and theorems) in EventB to JML
	public String machineInvariants(IRodinFile rodinFile, String machineName) throws RodinDBException {
		StringBuffer res = new StringBuffer("");
		ISCInvariant[] invariants =  utils.fetchInvariants(rodinFile,machineName);
		if (invariants.length > 0){
			res = new StringBuffer("/*@ public invariant\n");
		}
		String trans;
		for(int i=0; i < invariants.length; i++){
			//PrintTrace("Inv: "+ invariants[i].getLabel());
			//translationTrace(invariants[i].getLabel(),"inv1");
			trans = "\t " + transPred(invariants[i].getPredicateString());
			if (i != invariants.length-1){
				trans += " &&\n";
			}

			res.append(trans);
		}
		if (invariants.length > 0)
			res.append("; @*/\n");

		//Retrieving all theorems
		ISCInvariant[] theorems =  utils.fetchMachineTheorems(rodinFile,machineName);
		if (theorems.length > 0)
			res.append("/*@ public invariant_redundantly\n");
		trans = "";
		for(int i=0; i < theorems.length; i++){
			trans = "\t " + transPred(theorems[i].getPredicateString());
			if (i != theorems.length-1){
				trans += " &&\n";
			}

			res.append(trans);
		}
		if (theorems.length > 0)
			res.append("; @*/\n");
		return res.toString();
	}

	// Returns machine 'machineName' variants in EventB to JML
	public String machineVariants(IRodinFile rodinFile, String machineName) throws RodinDBException {
		StringBuffer res = new StringBuffer("");
		ISCVariant[] variants = utils.fetchVariants(rodinFile, machineName);
		for (ISCVariant variant : variants)
			res.append(variant.getExpressionString());
		return res.toString();
	}

	//Returns variable's type
	public String varType(ISCVariable var) throws RodinDBException{
		final FormulaFactory f = FormulaFactory.getDefault();
		//TODO: is missing when the type is a carry set

		//translationTrace(var.getElementName(),"busSta");
		String varType = getType(var.getElementName(), var.getType(f).toString());

		return varType;
	}

	//Returns variable (var) in B into JML.
	public String variable(ISCVariable var) throws RodinDBException{
		String res = "\t public model ";
		//translationTrace(var.getIdentifierString(), "yy");
		String v = res + varType(var) + " " + var.getIdentifierString() + ";\n";
		Pred2JML.as = false;
		return v;
	}

	// Returns machine 'machineName' variables in EventB to JML
	public String machineVariables(IRodinFile rodinFile, String machineName) throws RodinDBException {
		/*
		 * Gets all the variables of the machine. It stores them and
		 * the type of each variable is calculated when the invariants
		 * are being translated.
		 */
		StringBuffer res = new StringBuffer("/*@ ");
		ISCVariable[] variables = utils.fetchMachineVariables(rodinFile,machineName);
		for (ISCVariable var : variables){
			res.append(variable(var));
		}
		res.append("@*/");
		return res.toString();
	}

	// Returns event 'event' refine clause of machine 'machineName' in EventB to JML
	public String eventRefines(ISCEvent event) throws RodinDBException {
		System.out.println("================ Refines");
		StringBuffer res = new StringBuffer("");
		ISCRefinesEvent[] evtRefs = utils.fetchEvtRefines(event);
		for (ISCRefinesEvent evtRef : evtRefs)
			res.append(evtRef.getElementName());
		return res.toString();
	}

	public String ParameterType(ISCParameter param) throws RodinDBException {
		final FormulaFactory f = FormulaFactory.getDefault();

		String parameterType = getType(param.getElementName(), param.getType(f).toString());

		return parameterType;
	}

	// Returns event 'event' parameters of machine 'machineName' in EventB to JML
	public HashMap<String,String> eventParameters(ISCEvent event) throws RodinDBException {
		HashMap<String,String> parameter = new HashMap<String,String>();
		ISCParameter[] evtParameters = utils.fetchEvtParameters(event);
		for( ISCParameter par: evtParameters){
			String p = par.getElementName();
			String Type = ParameterType(par);
			parameter.put(p, Type);
		}
		return parameter;
	}

	// Returns event 'event' guards of machine 'machineName' in EventB to JML
	public String eventGuards(ISCEvent event) throws RodinDBException {
		StringBuffer res = new StringBuffer("");
		ISCGuard[] evtGuards = utils.fetchEvtGuards(event);
		for (int i=0; i<evtGuards.length;i++){
			//System.out.println("===> " + i);

			/*if (event.getLabel().equals("VOBC_special_transit_availability"))
				translationTrace(evtGuards[i].getLabel(), "grd8");
			else
				Pred2JML.as = false;*/
			res.append(transPred(evtGuards[i].getPredicateString()));
			if (i < evtGuards.length-1){
				res.append(" && ");
			}
		}
		if (evtGuards.length > 1){
			return "(" + res.toString() + ")";
		}else
			return res.toString();
	}

	// Returns event 'event' witnesses of machine 'machineName' in EventB to JML
	public String eventWitnesses(ISCEvent event) throws RodinDBException {
		System.out.println("================ Witnesses");
		StringBuffer res = new StringBuffer("");
		ISCWitness[] evtWitnesses = utils.fetchEvtWitnesses(event);
		for (ISCWitness evtWitness : evtWitnesses)
			res.append(evtWitness.getElementName());
		return res.toString();
	}

	// Returns event 'event' actions of machine 'machineName' in EventB to JML
	public String eventActions(ISCEvent event) throws RodinDBException {
		StringBuffer res = new StringBuffer("");
		vars = new ArrayList<String>();
		ISCAction[] evtActions =  utils.fetchEvtActions(event);
		int i = evtActions.length;
		for (ISCAction evtAction : evtActions){
			PrintTrace(evtAction.getLabel());
			if (event.getLabel().equals("evt1"))
				translationTrace(evtAction.getLabel(), "act1");
			else
				Pred2JML.as = false;
			HashMap<String,ArrayList<String>> assi = transAssig(evtAction.getAssignmentString());
			//Pred2JML.as = false;
			//translationTrace(evtAction.getLabel(), "act1");
			for (String var : assi.keySet()){
				System.out.println("||=== " + var + " - " + getInternalType(var));
				vars.add(var);
				String tt = getInternalType(var);
				if (assi.get(var).get(0).equals("BECOMES")){
					System.out.println("BECOMES");
					res.append(assi.get(var).get(1));
				}else
					if (tt.equals(jmlType.intT) || tt.equals(jmlType.boolT)){
						System.out.println("type is either int or bool");
						res.append(" " + var + " == \\old(" + assi.get(var).get(1) + ")");
					}else if (tt.equals(jmlType.setT) || tt.equals(jmlType.relT)){
						System.out.println("type is either brel or bset");
						res.append(" " + var + ".equals(\\old(" + assi.get(var).get(1) + "))");
					}else{
						System.out.println("no type");
						res.append("\t FIXIT - B2JML - eventActions");
					}
				if (i > 1){
					res.append(" && ");
				} i--;
			}
		}
		return res.toString();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public void print(String s){
		System.out.println(s);
	}
	public void print(int s){
		System.out.println(s);
	}

	public void translationTrace(String label, String s){
		if (label.equals(s))
			Pred2JML.as = true;
		else Pred2JML.as = false;
	}

	public boolean pt = true;
	public void PrintTrace(String s){
		if (pt)
			System.out.println(s);
	}

	private ISelection selection;

	private IRodinFile getSelectedComponent() {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				return EventBPlugin.asEventBFile(ssel.getFirstElement());
			}
		}
		return null;
	}
}