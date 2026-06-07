package eventb2jml_plugin.machine;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;


import org.eventb.core.ast.Assignment;
import org.eventb.core.ast.AssociativeExpression;
import org.eventb.core.ast.AssociativePredicate;
import org.eventb.core.ast.AtomicExpression;
import org.eventb.core.ast.BecomesEqualTo;
import org.eventb.core.ast.BecomesMemberOf;
import org.eventb.core.ast.BecomesSuchThat;
import org.eventb.core.ast.BinaryExpression;
import org.eventb.core.ast.BinaryPredicate;
import org.eventb.core.ast.BoolExpression;
import org.eventb.core.ast.BoundIdentDecl;
import org.eventb.core.ast.BoundIdentifier;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.ExtendedExpression;
import org.eventb.core.ast.ExtendedPredicate;
import org.eventb.core.ast.Formula;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.IParseResult;
import org.eventb.core.ast.ISimpleVisitor2;
import org.eventb.core.ast.IntegerLiteral;
import org.eventb.core.ast.LanguageVersion;
import org.eventb.core.ast.LiteralPredicate;
import org.eventb.core.ast.MultiplePredicate;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.PredicateVariable;
import org.eventb.core.ast.QuantifiedExpression;
import org.eventb.core.ast.QuantifiedPredicate;
import org.eventb.core.ast.RelationalPredicate;
import org.eventb.core.ast.SetExtension;
import org.eventb.core.ast.SimplePredicate;
import org.eventb.core.ast.Type;
import org.eventb.core.ast.UnaryExpression;
import org.eventb.core.ast.UnaryPredicate;


public class Pred2JML implements ISimpleVisitor2 {
	
	ArrayList<String> boundIdentifiers = new ArrayList<String>();
	
	/**************/
	public String tryType;
	/**************/
	
	
	boolean isaSubset;
	
	
	String translation;
	
	/*** Regarding to predicate Translation ***/ 
	private boolean predTrans;
	
	Stack<String> varFuncRel=new Stack<String>();
	
	/*** Regarding to assignment translation ***/
	private boolean assignmentTrans;
	private String assignamentType;
	// HashMap<String, ArrayList<String>> -> <name, <translation, Type>>
	private HashMap<String, ArrayList<String>> transAssignment;
	
	
	/*** Regarding to type variables translation ***/
	private boolean gettingType;
	// Variable (constant) types is stored in varType dictionary.
	// HashMap<Var name,type> varType;
	public HashMap<String,jmlType> varType;
	public jmlType currentType = new jmlType();
	
	public String currentVar = "";
	
	private boolean boolExp = false;
	
	
	public String typeConverseSetExt = "";
	
	Pred2JML(){
		predTrans = false;
		varFuncRel.clear();
		assignmentTrans = false;
		assignamentType = "";
		transAssignment = new HashMap<String, ArrayList<String>>();
		gettingType = false;
		tryType = "";
		varType = new HashMap<String,jmlType>();
	}
	
	public String getInternalType(String varName){
		if (varType.containsKey(varName)){
			return varType.get(varName).getInternalType();
		}else
			return "none";
	}
	
	public String getJmlType(String varName){
		if (varType.containsKey(varName)){
			return varType.get(varName).getJmlType();
		}else
			return "none";
	}
	
	public void setVarTypeSet(String name){
		jmlType type = new jmlType(name, jmlType.setT);
		type.update(jmlType.intT);
		varType.put(name, type);
	}
	
	
	public String getvariableType(String varName, String varDef, boolean storeVar){
		tryType = "";
		if (varType.containsKey(varName)){
			return varType.get(varName).getJmlType();
		}else{
			currentType = new jmlType();
			boolean tmpGettingType = gettingType;
			boolean tmpPredTrans = predTrans;
			boolean tmpAssignmentTrans = assignmentTrans;
			String tmpTranslation = translation;
			boolExp = false;
			gettingType = true;
			predTrans = false;
			assignmentTrans = false;
			translation = "";
			nestedType = false;
			Exp(varDef);
			gettingType = tmpGettingType;
			predTrans = tmpPredTrans;
			assignmentTrans = tmpAssignmentTrans;
			ArrayList<String> d = new ArrayList<String>();
			d.add(translation);
			d.add("varInternalType");
			String tmp3 = translation;
			translation = tmpTranslation;
			if (storeVar && !boolExp){
				varType.put(varName, currentType);
			}
			return tmp3;
		}
	}
	
	public String Pre(String predicate){
		tryType = "";
		translation = "";
		boolean tmpPredTrans = predTrans;
		boolean tmpAssignmentTrans = assignmentTrans;
		boolean tmpGettingType = gettingType;
		boolExp = false;
		predTrans = true;
		assignmentTrans = false;
		gettingType = false;
		varFuncRel.clear();
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parsePredicate(predicate, LanguageVersion.LATEST,null);
		final Predicate p = result.getParsedPredicate();
		p.accept(this);
		predTrans = tmpPredTrans;
		assignmentTrans = tmpAssignmentTrans;
		gettingType = tmpGettingType;
		return translation;

	}
	
	public void Exp(String expression){
		tryType = "";
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parseExpression(expression, LanguageVersion.LATEST,null);
		final Expression e = result.getParsedExpression();
		e.accept(this);
	}
	
	// Translates EventB assignments (var := Expression) into JML
	public HashMap<String,ArrayList<String>> Assignment(String assig){
		tryType = "";
		transAssignment = new HashMap<String,ArrayList<String>>();
		boolean tmpAssignmentTrans = assignmentTrans; 
		boolean tmpGettingType = gettingType;
		boolean tmpPredTrans = predTrans;
		assignmentTrans = true;
		gettingType = false;
		predTrans = false;
		boolExp = false;
		
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parseAssignment(assig, LanguageVersion.LATEST, null);
		
		final Assignment a = result.getParsedAssignment();		
		a.accept(this);
		assignmentTrans = tmpAssignmentTrans;
		gettingType = tmpGettingType;
		predTrans = tmpPredTrans;
		return transAssignment;
	}
	

	@Override
	public void visitBecomesEqualTo(BecomesEqualTo assignment) {
		tryType = "";
		Print("visitBecomesEqualTo");
		// not Predicate (???)
		
		FreeIdentifier[] ident = assignment.getAssignedIdentifiers();
		Expression[] exp = assignment.getExpressions();
		for (int i=0; i < ident.length;i++){
			currentVar = ident[i].toString();
			translation = "";
			assignamentType = "";
			exp[i].accept(this);
			if (!boolExp){
				ArrayList<String> s = new ArrayList<String>();
				s.add(assignamentType);
				s.add(translation);
				Print("Variable to be stored in transAssignment (visitBecomesEqualTo): " + currentVar + " <");
				transAssignment.put(currentVar,s);
			}
			currentVar = "";
		}
	}

	@Override
	public void visitBecomesMemberOf(BecomesMemberOf assignment) {
		Print("\n visitBecomesMemberOf");
		// Becomes Member Of
		tryType = "";
		FreeIdentifier[] ident = assignment.getAssignedIdentifiers();
		Expression exp = assignment.getSet();
		String tmp = "";
		translation = "";
		exp.accept(this);
		String t = "";
		for (FreeIdentifier var: ident){
			String var_type = varType.get(var.getName()).getJmlType(); 
			String var_name = var.getName();
			if (var_type.equals(jmlType.intT) || var_type.equals(jmlType.boolT)){
				t = "(\\exists " + var_type + " " + var_name + "_localVar; " + translation + 
						".has(" + var_name + "_localVar); " + var_name + " == " + var_name + "_localVar)";
			}else if (var_type.equals(jmlType.setT) || var_type.equals(jmlType.relT)){
				t = "(\\exists " + var_type + " " + var_name + "_localVar; " + 
						var_name + "_localVar.isSubset(" + translation + "); " + var_name + ".equals(" + var_name + "_localVar))";
			}
		}
		
		translation = t;
		
		/*******/
		if (exp instanceof UnaryExpression){
			if (exp.getTag() == Formula.POW){
				for (FreeIdentifier var: ident){
				}
			}
		}
		/*******/
		
		assignamentType = "BECOMES";
		ArrayList<String> s = new ArrayList<String>();
		s.add(assignamentType);
		s.add(translation);
		for (FreeIdentifier var: ident){
			if (!boolExp){
				Print("Variable to be stored in transAssignment (visitBecomesMemberOf): " + var.toString() + " <");
				transAssignment.put(var.toString(), s);
			}
		}
	}

	@Override
	public void visitBecomesSuchThat(BecomesSuchThat assignment) {
		Print("\n visitBecomesSuchThat");
	}

	@Override
	public void visitBoundIdentDecl(BoundIdentDecl boundIdentDecl) {
		Print("\n visitBoundIdentDecl");
		//store the bounded variables
		if (predTrans){
			translation += boundIdentDecl.getName();
		}
		//boundIdentifiers.push(boundIdentDecl.getName());
		boundIdentifiers.add(0,boundIdentDecl.getName());
		
	}

	@Override
	public void visitAssociativeExpression(AssociativeExpression expression) {
		Print("\n visitAssociativeExpression");
		switch (expression.getTag()){
		case 	Formula.BUNION:
			Print("BUNION");
			
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (i > 1){
						translation += ".union(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + translation;
			}
			break;
		case 	Formula.BINTER:
			Print("BINTER");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (i > 1){
						translation += ".intersection(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + translation;
			}
			break;
		case 	Formula.BCOMP:
			Print("BCOMP");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (i > 1){
						translation += ".backwardCompose(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + translation;
			}
			break;
		case 	Formula.FCOMP:
			Print("FCOMP");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (i > 1){
						translation += ".compose(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + translation;
			}
			break;
		case 	Formula.OVR:
			Print("OVR");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (i > 1){
						translation += ".override(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + translation;
			}
			break;
		case 	Formula.PLUS:
			Print("PLUS");
			int i = expression.getChildren().length;
			for (Expression exprs : expression.getChildren()){
				exprs.accept(this);
				if (i > 1){
					translation += " + ";
				} i--;
			}
			break;
		case 	Formula.MUL:
			Print("MUL");
			int i1 = expression.getChildren().length;
			for (Expression exprs : expression.getChildren()){
				exprs.accept(this);
				if (i1 > 1){
					translation += " * ";
				} i1--;
			}
			break;
		}
	}

	@Override
	public void visitAtomicExpression(AtomicExpression expression) {
		Print("\n visitAtomicExpression");
		
		/*{INTEGER, NATURAL, NATURAL1, BOOL, TRUE, FALSE, EMPTYSET, KPRED, KSUCC,
		  KPRJ1_GEN, KPRJ2_GEN, KID_GEN}.*/
		
		switch (expression.getTag()){
		case	Formula.INTEGER:
			Print("INTEGER");
			if (predTrans){
				translation += "INT.instance";
			}else
			if (gettingType){
				translation += "Integer";
				if (currentType.d){
					currentType = new jmlType("SHT",jmlType.intT);
				}else{
					if (currentType.getInternalType().equals(jmlType.relT) ||
							currentType.getInternalType().equals(jmlType.setT)){
						currentType.update(jmlType.intT);
					}
				}
			}else
			if (assignmentTrans){
				translation += "Integer";
			}
			break;
		case	Formula.BOOL:
			Print("BOOL");
			if (predTrans || assignmentTrans){
				translation += "BOOL.instance";
			}
			else
			if (gettingType){
				if (currentType.d){
					currentType = new jmlType("SHT",jmlType.boolT);
				}else{
					if (currentType.getInternalType().equals(jmlType.relT) ||
							currentType.getInternalType().equals(jmlType.setT)){
						currentType.update(jmlType.boolT);
					}
				}
				
				translation += "Boolean";
			}
			break;
		case	Formula.TRUE:
			Print("TRUE");
			if (assignmentTrans){
				translation += "true";
				if (assignamentType.equals("")){
					assignamentType = jmlType.NATIVE;
				}
			}else if(predTrans){
				translation += "true";
			}
			if (mapstoType.size() > 0){
				updateMapstoType("boolean");
			}
			break;
		case	Formula.FALSE:
			Print("FALSE1");
			if (assignmentTrans){
				translation += "false";
				if (assignamentType.equals("")){
					assignamentType = jmlType.NATIVE;
				}
			}else if(predTrans){
				translation += "false";
			}
			if (mapstoType.size() > 0){
				updateMapstoType("boolean");
			}
			break;
		case	Formula.NATURAL:
			Print("NATURAL");
			if (predTrans || assignmentTrans){
				translation += "NAT.instance";
			}
			break;
		case	Formula.NATURAL1:
			Print("NATURAL1");
			if (predTrans){
				translation += "NAT1.instance";
			}else if (assignmentTrans){
				translation += "NAT1";
			}
			break;
		case	Formula.EMPTYSET:
			Print("EMPTYSET");
			if (assignmentTrans){
				if (assignamentType.equals("")){
					assignamentType = "EMPTY";
				}
				translation += "BSet.EMPTY";
			}else if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				translation += "BSet.EMPTY";
			}
			break;
		case	Formula.KPRED:
			Print("KPRED");
			break;
		case	Formula.KSUCC:
			Print("KSUCC");
			break;
		case	Formula.KPRJ1_GEN:
			Print("KPRJ1_GEN");
			break;
		case	Formula.KPRJ2_GEN:
			Print("KPRJ2_GEN");
			break;
		case	Formula.KID_GEN:
			Print("KID_GEN");
			translation += "(new ID())";
			break;
		}		
	}
	
	private ArrayList<String> mapstoType = new ArrayList<String>();
	private boolean nestedType = false;

	@Override
	public void visitBinaryExpression(BinaryExpression expression) {
		Print("\n visitBinaryExpression");
		// Binary Expression (check)
		// expression {} expression
		switch (expression.getTag()){
		case Formula.MINUS:
			Print("MINUS");
			updateMapstoType("Integer");
			if (assignmentTrans || predTrans){
				expression.getLeft().accept(this);
				translation += " - ";
				expression.getRight().accept(this);
			}
			break;
		case Formula.MAPSTO:
			Print("MAPSTO");
			String tmp2 = translation;
			translation = "";
			String l = varType.get(currentVar).getJmlType();
			tmp2 = tmp2.replace("?TYPE?", l);
			tmp2 = tmp2.replace("BSet<", "");
			tmp2 = tmp2.replaceFirst(">", "");
			translation += "(new "+ l.replace("BRelation", "JMLEqualsEqualsPair") +"(";
			expression.getLeft().accept(this);
			translation += ",";
			expression.getRight().accept(this);
			translation += "))";
			
			translation = tmp2 + translation;
			break;
		case Formula.DIV:
			Print("DIV");
			updateMapstoType("Integer");
			if (assignmentTrans || predTrans){
				expression.getLeft().accept(this);
				translation += " / ";
				expression.getRight().accept(this);
			}
			break;
		case Formula.MOD:
			Print("MOD");
			updateMapstoType("Integer");
			if (assignmentTrans || predTrans){
				expression.getLeft().accept(this);
				translation += " % ";
				expression.getRight().accept(this);
			}
			break;
		case Formula.EXPN:
			Print("EXPN");
			expression.getLeft().accept(this);
			expression.getRight().accept(this);
			break;
		case Formula.REL:
			Print("REL");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
					// create restrictions for domain and Range.
				    boolean nested = false;
				    if (isFunRel(expression.getLeft().getTag())){
				    	nested = true;
				        //it is a nested function. We need to translate the type of the new Func/Rel
				        String newType = getvariableType("",expression.getLeft().getType().toString(), false);
				        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
				        newType = newType.replaceFirst("BSet<", "");
				        //and eliminate the last '>'
				        newType = newType.substring(0, newType.length()-1);
				    					
				        translation += varFuncRel.peek() + ".domain().isSubset(" +
				    	"new " + newType + "()" + ") && ";
				    }else{
				        translation += varFuncRel.peek() + ".domain().isSubset(";
				        nested = false;
				    }
					
					String tmp = translation;
					String rightSide = "";
					String leftSide = "";
					translation = "";
					
					//used in case of nested function
				    varFuncRel.push(varFuncRel.peek() + ".domain()");
					
					expression.getLeft().accept(this);
					leftSide = translation;
					translation = tmp + translation;
				    varFuncRel.pop();
				    
				    if (!nested)
				    	translation += ") && ";
				        else
				    	translation += " && ";
				    
				    if (isFunRel(expression.getRight().getTag())){
				    	nested = true;
				        //it is a nested function. We need to translate the type of the new Func/Rel
				        String newType = getvariableType("",expression.getRight().getType().toString(),false);
				        //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				        newType = newType.replaceFirst("BSet<", "");
				        //and eliminate the last '>'
				        newType = newType.substring(0, newType.length()-1);
				    					
				        translation += varFuncRel.peek() + ".range().isSubset(" +
				    	"new " + newType + "()" + ") && ";
				    }else{
				        translation += varFuncRel.peek() + ".range().isSubset(";
				        nested = false;
				    }
				    
					tmp = translation;
					translation = "";
					varFuncRel.push(varFuncRel.peek() + ".range()");
					expression.getRight().accept(this);
					varFuncRel.pop();
					rightSide = translation;
					translation = tmp + translation;
					if (!nested){
						translation += ")";
					}
					
					//Checks if either left or right side visits a ID
					if (expression.getRight().getTag() == Formula.KID_GEN){
						if (translation.contains("??")){
							translation = translation.replace("??", leftSide);
						}
					}else if(expression.getLeft().getTag() == Formula.KID_GEN){
						if (translation.contains("??")){
							translation = translation.replace("??", rightSide);
						}
					}
					if (varFuncRel.size() == 1){
						varFuncRel.pop();
						if (isaSubset){
							translation += ")";
						}
					}
				//}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TREL:
			Print("TREL");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().equals(";
				    nested = false;
				}
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				leftSide = translation;
				translation = tmp + translation;
				varFuncRel.pop();
				
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";
								
				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().isSubset(";
				    nested = false;
				}
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.SREL:
			Print("SREL");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
				    translation += varFuncRel.peek() + ".domain().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().isSubset(";
				    nested = false;
				}
				
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				leftSide = translation;
				translation = tmp + translation;
				varFuncRel.pop();
				
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";
								
				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().equals(";
				    nested = false;
				}
				
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.STREL:
			Print("STREL");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().equals(";
				    nested = false;
				}
				
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";
				
				
				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().equals(";
				    nested = false;
				}
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.PFUN:
			Print("PFUN");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && ";
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().isSubset(";
				    nested = false;
				}
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";
				
				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().isSubset(";
				    nested = false;
				}
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TFUN:
			Print("TFUN");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
					
				}
				
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && ";
				
				// create restrictions for domain and Range.
				
				//if the domain is a function or relation we cannot put 'equals' or 'subset'
				if (isFunRel(expression.getLeft().getTag())){
					nested = true;
					//it is a nested function. We need to translate the type of the new Func/Rel
					String newType = getvariableType("",expression.getLeft().getType().toString(),false);
					//since the Func/Rel are defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					
					translation += varFuncRel.peek() + ".domain().equals(" +
							"new " + newType + "()" + ") && ";
					
				}else{
					translation += varFuncRel.peek() + ".domain().equals(";
					nested = false;
				}
				
				
				
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				
				expression.getLeft().accept(this);
				leftSide = translation;
				translation = tmp + translation;
				varFuncRel.pop();
				
				if (!nested)
					translation += ") && ";
				else
					translation += " && ";
				
				if (isFunRel(expression.getRight().getTag())){
					nested = true;
					//it is a nested function. We need to translate the type of the new Func/Rel
					String newType = getvariableType("",expression.getRight().getType().toString(),false);
					//since the Func/Rel are defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					
					translation += varFuncRel.peek() + ".range().isSubset(" +
							"new " + newType + "()" + ") && ";
					
				}else{
					translation += varFuncRel.peek() + ".range().isSubset(";
					nested = false;
				}
				tmp = translation;
				translation = "";
				
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
					translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.PINJ:
			Print("PINJ");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && " + varFuncRel.peek() +".inverse().isaFunction() && ";
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().isSubset(";
				    nested = false;
				}
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";
				
				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().isSubset(";
				    nested = false;
				}
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TINJ:
			Print("TINJ");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && "+ varFuncRel.peek() +".inverse().isaFunction() && ";
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().equals(";
				    nested = false;
				}
				
				
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";

				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().isSubset(";
				    nested = false;
				}
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.PSUR:
			Print("PSUR");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && ";
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().isSubset(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().isSubset(";
				    nested = false;
				}
				
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";

				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().equals(";
				    nested = false;
				}
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TSUR:
			Print("TSUR");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && ";
				
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().equals(";
				    nested = false;
				}
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";

				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().equals(";
				    nested = false;
				}
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TBIJ:
			Print("TBIJ");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
			        //since the Func/Rel is defined as Power Set, we need to capture just the inside type
			        newType = newType.replaceFirst("BSet<", "");
			        //and eliminate the last '>'
			        newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				boolean nested = false;
				translation += varFuncRel.peek() +  ".isaFunction() && " + varFuncRel.peek() + ".inverse().isaFunction() && ";
				
				// create restrictions for domain and Range.
				if (isFunRel(expression.getLeft().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getLeft().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".domain().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".domain().equals(";
				    nested = false;
				}
				
				
				String tmp = translation;
				String rightSide = "";
				String leftSide = "";
				translation = "";
				//used in case of nested function
				varFuncRel.push(varFuncRel.peek() + ".domain()");
				expression.getLeft().accept(this);
				varFuncRel.pop();
				leftSide = translation;
				translation = tmp + translation;
				if (!nested)
				    translation += ") && ";
				else
				    translation += " && ";

				if (isFunRel(expression.getRight().getTag())){
				    nested = true;
				    //it is a nested function. We need to translate the type of the new Func/Rel
				    String newType = getvariableType("",expression.getRight().getType().toString(),false);
				    //since the Func/Rel are defined as Power Set, we need to capture just the inside type
				    newType = newType.replaceFirst("BSet<", "");
				    //and eliminate the last '>'
				    newType = newType.substring(0, newType.length()-1);
									
				    translation += varFuncRel.peek() + ".range().equals(" +
					"new " + newType + "()" + ") && ";
									
				}else{
				    translation += varFuncRel.peek() + ".range().equals(";
				    nested = false;
				}
				
				tmp = translation;
				translation = "";
				varFuncRel.push(varFuncRel.peek() + ".range()");
				expression.getRight().accept(this);
				varFuncRel.pop();
				rightSide = translation;
				translation = tmp + translation;
				if (!nested){
				    translation += ")";
				}
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
				    varFuncRel.pop();
				    if (isaSubset){
						translation += ")";
					}
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.SETMINUS:
			// Set difference
			
			//TODO: updateMapstoType("??");
			Print("SETMINUS");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				String rightSide = "";
				String leftSide = "";
				expression.getLeft().accept(this);
				leftSide = translation;
				translation = tmp + translation + ".difference(";
				tmp = translation;
				translation = "";
				expression.getRight().accept(this);
				rightSide = translation;
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}
			break;
		case Formula.DOMRES:
			Print("DOMRES");
			if (predTrans || assignmentTrans){
				//TODO: updateMapstoType("??");
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				expression.getRight().accept(this);
				String rightSide = translation;
				String leftSide = "";
				translation = tmp + translation + ".restrictDomainTo(";
				tmp = translation;
				translation = "";
				expression.getLeft().accept(this);
				typeConverseSetExt = "";
				leftSide = translation;
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}
			break;
		case Formula.DOMSUB:
			Print("DOMSUB");
			if (predTrans || assignmentTrans){
				//TODO: updateMapstoType("??");
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				String rightSide = "";
				String leftSide = "";
				expression.getRight().accept(this);
				rightSide = translation;
				translation = tmp + translation + ".domainSubtraction(";
				tmp = translation;
				translation = "";
				expression.getLeft().accept(this);
				typeConverseSetExt = "";
				leftSide = translation;
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}
			break;
		case Formula.RANRES:
			Print("RANRES");
			if (predTrans || assignmentTrans){
				//TODO: updateMapstoType("??");
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				String rightSide = "";
				String leftSide = "";
				expression.getLeft().accept(this);
				leftSide = translation;
				translation = tmp + translation + ".restrictRangeTo(";
				tmp = translation;
				translation = "";
				expression.getRight().accept(this);
				typeConverseSetExt = "";
				rightSide = translation;
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}			
			break;
		case Formula.RANSUB:
			Print("RANSUB");
			if (predTrans || assignmentTrans){
				//TODO: updateMapstoType("??");
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				String leftSide = "";
				String rightSide = "";
				expression.getLeft().accept(this);
				leftSide = translation;
				translation = tmp + translation + ".rangeSubtraction(";
				tmp = translation;
				translation = "";
				expression.getRight().accept(this);
				typeConverseSetExt = "";
				rightSide = translation;
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}			
			break;
		case Formula.UPTO:  
			Print("UPTO");
			if (predTrans || assignmentTrans){
				//TODO: updateMapstoType("??");
				if (predTrans && tryType.equals("")){
					tryType = jmlType.SET;
				}
				translation += "(new Range(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += "))";
			}
			break;
		case Formula.RELIMAGE:
			Print("RELIMAGE");
			String tmp1 = translation;
			translation = "";
			expression.getLeft().accept(this);
			translation = tmp1 + translation + ".image(";
			expression.getRight().accept(this);
			translation += ")";
			break;
		case Formula.CPROD:
			Print("CPROD");
			if (gettingType){
				if (!nestedType){
					translation += "BRelation<";
					nestedType = true;
					if (currentType.d){
						currentType = new jmlType("STH",jmlType.relT);
					}else{
						currentType.update(jmlType.relT);
					}
				}else{
					currentType.update(jmlType.relT);
					translation += "BRelation<";
				}
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ">";
				//nestedType = false;
			}else if(predTrans || assignmentTrans){
				if (predTrans && tryType.equals("")){
					tryType = jmlType.SET;
				}
				translation += "Utils.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;	
		case Formula.DPROD:
			Print("DPROD");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				expression.getLeft().accept(this);
				translation = tmp + translation + ".directProd(";
				expression.getRight().accept(this);
				translation += tmp + ")";
			}
			break;
		case Formula.PPROD:
			Print("PPROD");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				expression.getLeft().accept(this);
				translation = tmp + translation + ".parallel(";
				expression.getRight().accept(this);
				translation += tmp + ")";
			}
			break;
		case Formula.FUNIMAGE:
			Print("FUNIMAGE");
			String tmp = translation;
			translation = "";
			expression.getLeft().accept(this);
			translation = tmp + translation + ".apply(";
			expression.getRight().accept(this);
			translation += ")";
			break;
		}
	}

	@Override
	public void visitBoolExpression(BoolExpression expression) {
		Print("\n visitBoolExpression "); //bool(predicate)
		boolExp = true;
		Print("boolExp ===========> " + boolExp);
		translation += "(";
		expression.getPredicate().accept(this);
		translation += ")";
	}

	@Override
	public void visitIntegerLiteral(IntegerLiteral expression) {
		Print("\n visitIntegerLiteral");
		// Integet Literal (check)
		// 0..9
		updateMapstoType("Integer");
		translation = translation.replace("?TYPE?", "Integer");
		if (predTrans){
			translation += expression.getValue();
		}else if(assignmentTrans){
			if (assignamentType.equals("")){
				assignamentType = jmlType.NATIVE;
			}
			translation += expression.getValue();
		}
	}

	@Override
	public void visitQuantifiedExpression(QuantifiedExpression expression) {
		Print("\n visitQuantifiedExpression");
		// Quantified Expression (can't find 'comprehension Set')
		// {union (bigU), intersection (bign)} varlist . predicate | expression
		switch (expression.getTag()){
		case 	Formula.QUNION:
			Print("QUNION");
			break;
		case 	Formula.QINTER:
			Print("QINTER");
			break;
		}
		for (BoundIdentDecl var: expression.getBoundIdentDecls()){
			//translation += var.toString();
		}
		//translation += '.';
		expression.getExpression().accept(this);
		//translation += '|';
		expression.getPredicate().accept(this);
	}
	
	
	@Override
	public void visitSetExtension(SetExtension expression) {
		Print("\n visitSetExtension");
		// Set Extension
		// '{' list_expression '}'
		if (predTrans || assignmentTrans){
			if (predTrans && tryType.equals("")){
				tryType = jmlType.SET;
			}
			if (assignmentTrans && assignamentType.equals("")){
				assignamentType = jmlType.SET;
			}
			String t = "";
			
			String tmp = translation;
			translation = "";
			
			t = "?TYPE?";	
			
			
			if (translation.contains("?TYPE?")){
				translation = translation.replace("?TYPE?", "BSet<"+t+">");
			}else{
				if (t.contains("BRelation")){
					translation += "(new "+t+"(";
				}else{
					translation += "(new BSet<"+t+">(";
				}
			}
			
			for (int exp=0; exp < expression.getMembers().length;exp++){
				//Parse each expression.
				expression.getMembers()[exp].accept(this);
				if (exp < expression.getMembers().length-1){
					translation += ",";
				}else{
					translation += "))";
				}
			}
			translation = tmp + translation;
		}else{
			translation += "{";
			for (Expression exp: expression.getMembers()){
				//Parse each expression.
				exp.accept(this);
				translation += ",";
			}
			translation += "}";
		}
		
	}

	@Override
	public void visitUnaryExpression(UnaryExpression expression) {
		// Unary Expressions
		//	Ident(expression)
		
		Print("\n visitUnaryExpression");
		switch (expression.getTag()){
		case 	Formula.UNMINUS:
			Print("UNMINUS");
			expression.getChild().accept(this);
			break;
		case 	Formula.KCARD:
			Print("KCARD");
			expression.getChild().accept(this);
			translation += ".int_size()";
			break;
		case 	Formula.POW:
			Print("POW");
			if (predTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				expression.getChild().accept(this);
				translation += ".pow()";
			}else if (gettingType){
				if (expression.getChild() instanceof BinaryExpression){
					if (expression.getChild().getTag() == Formula.CPROD){
						expression.getChild().accept(this);
					}
					else{
						translation += "FIXIT - Formula.POW";
						expression.getChild().accept(this);
					}
				}else{
					if (currentType.d){
						currentType = new jmlType("STH",jmlType.setT);
					}else{
						currentType.update(jmlType.setT);
					}
					translation += "BSet<";
					expression.getChild().accept(this);
					translation += ">";
				}
			}else if (assignmentTrans){
				translation += "(new BSet<Type>(";
				expression.getChild().accept(this);
				translation += ").pow()";
			}
			break;
		case 	Formula.POW1:
			Print("POW1");
			expression.getChild().accept(this);
			break;
		case 	Formula.KUNION:
			Print("KUNION");
			expression.getChild().accept(this);
			break;
		case 	Formula.KINTER:
			Print("KINTER");
			expression.getChild().accept(this);
			break;
		case 	Formula.KDOM:
			Print("KDOM");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".domain()";
			}else{
				expression.getChild().accept(this);
			}
			break;
		case 	Formula.KRAN:
			Print("KRAN");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String d = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = d + translation + ".range()";
			}else{
				expression.getChild().accept(this);
			}
			break;
		case 	Formula.KMIN:
			Print("KMIN");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".min()";
			}else{
				expression.getChild().accept(this);
			}
			break;
		case 	Formula.KMAX:
			Print("KMAX");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".max()";
			}
			break;
		case 	Formula.CONVERSE:
			Print("CONVERSE");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".inverse()";
			}
			break;
		case 	Formula.KPRJ1_GEN:
			Print("KPRJ1_GEN");
			expression.getChild().accept(this);
			break;
		case 	Formula.KPRJ2_GEN:
			Print("KPRJ2_GEN");
			expression.getChild().accept(this);
			break;
		case 	Formula.KID_GEN:
			Print("KID_GEN");
			expression.getChild().accept(this);
			break;
		}
	}

	//TODO
	private void updateMapstoType(String u){
		int last = mapstoType.size();
		if (last > 0){
			String type = "";
			if(u.contains("INT") || u.contains("NAT")){
				type = "Integer";
			}else if(u.contains("BOOL")){
				type = "boolean";
			}else{
				type = u;
			}
			if (mapstoType.get(last-2).contains("??")){
				mapstoType.set(last-2, type);
			}else if (mapstoType.get(last-1).contains("??")){
				mapstoType.set(last-1, type);
			}
		}
	}
	
	@Override
	public void visitBoundIdentifier(BoundIdentifier identifierExpression) {
		Print("\n visitBoundIdentifier");
		if (predTrans){
			translation += boundIdentifiers.get(identifierExpression.getBoundIndex());
			updateMapstoType(varType.get(boundIdentifiers.get(identifierExpression.getBoundIndex())).getJmlType());
		}
	}

	@Override
	public void visitFreeIdentifier(FreeIdentifier identifierExpression) {
		Print("\n visitFreeIdentifier");
		Print("\t -- " + identifierExpression.getName());
		
		String typeVar = varType.get(identifierExpression.getName()).getJmlType();
		
		jmlType tt = varType.get(identifierExpression.getName());
		Print("tt: " + tt.getJmlType());
		if (tt.getInternalType().equals(jmlType.relT)){
			translation = translation.replace("new BSet<?TYPE?>", "new " + tt.getJmlType());
		}else{
			Print("=====> " + tt.getName() + " == " + tt.getJmlType());
			if (tt.getJmlType().equals(jmlType.setT)){
				translation = translation.replace("?TYPE?", tt.getSetType().getJmlType());
			}else{
				translation = translation.replace("?TYPE?", tt.getJmlType());
			}
		}
			
		
		if (gettingType) {
			Type type = identifierExpression.getType();
			String tmp;
			if (type == null){
				tmp = jmlType.intT;
			}else{
				tmp = type.toString();
			}
			if (currentType.d){
				currentType = new jmlType(currentVar,tmp);
			}else{
				currentType.update(tmp);
			}
			
			translation += tmp;
			
		}else
		if (predTrans){
			translation += identifierExpression.getName();
		}else if (assignmentTrans){
			translation += identifierExpression.getName();
		}
		
		if (mapstoType.size() > 0) {
			Type type = identifierExpression.getType();
			if (type == null){
				updateMapstoType("Integer");
			}else{
				updateMapstoType(type.toString());
			}
		}
	}

	@Override
	public void visitAssociativePredicate(AssociativePredicate predicate) {
		Print("\n visitAssociativePredicate");
		// check again!
		switch (predicate.getTag()){
		case Formula.LAND:
			Print("LAND");
			int i = predicate.getChildren().length;
			for (Predicate pre : predicate.getChildren()){
				pre.accept(this);
				if (i > 1){
					translation += " && ";
				}i--;
			}
			break;
		case Formula.LOR:
			Print("LOR");
			int i2 = predicate.getChildren().length;
			for (Predicate pre : predicate.getChildren()){
				pre.accept(this);
				if (i2 > 1){
					translation += " || ";
				}i2--;
			}
			break;
		}
	}

	@Override
	public void visitBinaryPredicate(BinaryPredicate predicate) {
		Print("\n visitBinaryPredicate");
		//Binary predicate
		//	predicate {or/and/impl/equiv} predicate
		
		switch (predicate.getTag()){
		case Formula.LIMP:
			Print("LIMP");
			if (predTrans||assignmentTrans){
				translation += "(";
				predicate.getLeft().accept(this);
				translation += ") ==> (";
				predicate.getRight().accept(this);
				translation += ")";
			}
			break;
		case Formula.LEQV:
			Print("LEQV");
			if (predTrans){
				translation += "(";
				predicate.getLeft().accept(this);
				translation += ") <==> (";
				predicate.getRight().accept(this);
				translation += ")";
			}
			break;
		}
			
	}

	@Override
	public void visitLiteralPredicate(LiteralPredicate predicate) {
		Print("\n visitLiteralPredicate");
		switch (predicate.getTag()){
		case 	Formula.BTRUE:
			Print("BTRUE");
			translation += "true";
			break;
		case 	Formula.BFALSE:
			Print("BFALSE");
			translation += "false";
			break;
		}
	}

	@Override
	public void visitMultiplePredicate(MultiplePredicate predicate) {
		Print("\n visitMultiplePredicate");
		//don't know what MultiplePredicate is.
	}

	@Override
	public void visitQuantifiedPredicate(QuantifiedPredicate predicate) {
		Print("\n visitQuantifiedPredicate");
		// Quantified Predicate
		//	{forall/exists} varlist . predicate
		String par = "";
		switch (predicate.getTag()){
		case	Formula.FORALL:
			Print("FORALL");
			if (predTrans){
				String tmp = translation;
				translation = "";
				//translation += "(\\forall ";
				int i = predicate.getBoundIdentDecls().length;
				for (BoundIdentDecl var : predicate.getBoundIdentDecls()){
					//Has to get the bound variable and its type
					String tmp2 = "";
					tmp2 = translation;
					translation = "";
					var.accept(this);
					// Translation variable contains the bound variable name
					//TODO: checks the type
					
					//TODO we should not store that variable.. the parameter should be true
					String typeUnbounded = getvariableType(var.getName(), var.getType().toString(),true);
					
					if (typeUnbounded.equals("NAT1.instance") ||
							typeUnbounded.equals("NAT.instance") ||
							typeUnbounded.equals("INT.instance") ||
							typeUnbounded.equals("Integer")){
						typeUnbounded = "Integer";
					}else if (typeUnbounded.equals("BOOL.instance")){
						typeUnbounded = "Boolean";
					}else{
						typeUnbounded = "BSet<" + typeUnbounded + ">";
					}
					
					translation = tmp2 + " (\\forall " + typeUnbounded + " " + translation;
					par += ")";
					if (i > 1){
						 translation += "; ";
					}i--;
				}
				translation = tmp + translation + ";";
			}
			break;
		case	Formula.EXISTS:
			Print("EXISTS");
			if (predTrans){
				String tmp = translation;
				translation = "";
				//translation += "(\\exists ";
				
				int i = predicate.getBoundIdentDecls().length;
				for (BoundIdentDecl var : predicate.getBoundIdentDecls()){
					//Has to get the bound variable and its type
					String tmp2 = "";
					tmp2 = translation;
					translation = "";
					var.accept(this);
					// Translation variable contains the bound variable name
					//TODO: checks the type
					String typeUnbounded = getvariableType(var.getName(), var.getType().toString(),true);
					if (typeUnbounded.equals("NAT1.instance") ||
							typeUnbounded.equals("NAT.instance") ||
							typeUnbounded.equals("INT.instance") ||
							typeUnbounded.equals("Integer")){
						typeUnbounded = "Integer";
					}else if (typeUnbounded.equals("BOOL.instance")){
						typeUnbounded = "Boolean";
					}else{
						typeUnbounded = "BSet<" + typeUnbounded + ">";
					}
					
					translation = tmp2 + " (\\exists " + typeUnbounded + " " + translation;
					par += ")";
					if (i > 1){
						 translation += ";";
					}i--;
				}
				translation = tmp + translation + ";";
			}
			break;
		}
		predicate.getPredicate().accept(this);
		translation += par;
		int numBoundIdent = predicate.getBoundIdentDecls().length;
		boundIdentifiers.subList(0, numBoundIdent).clear();
	}

	@Override
	public void visitRelationalPredicate(RelationalPredicate predicate) {
		Print("\n visitRelationalPredicate");
		// Relational Predicate
		// expression	{= / <= / < / >= / > / : (belongs)} expression
		switch (predicate.getTag()){
		case 	Formula.EQUAL:
			Print("EQUAL");
			Print("predTrans: " + predTrans);
			Print("assignmentTrans: " + assignmentTrans);
			if (predTrans || boolExp){
				String tmp = translation;
				translation = "";
				tryType = "";
				predicate.getLeft().accept(this);
				String ident = translation;
				if (varType.containsKey(ident)){
					String tt = varType.get(ident).getInternalType();
					if (tt.equals(jmlType.relT) || tt.equals(jmlType.setT)){
						translation = tmp + ident + ".equals(";
						predicate.getRight().accept(this);
						translation += ")";
					}else{
					translation = tmp + translation + " == ";
					predicate.getRight().accept(this);
					}
				}else{
					translation = tmp + translation + " == ";
					predicate.getRight().accept(this);
				}
				tryType = "";
			}
			break;
		case 	Formula.NOTEQUAL:
			Print("NOTEQUAL");
			if (predTrans){
				String tmp = translation;
				translation = "";
				tryType = "";
				predicate.getLeft().accept(this);
				String ident = translation;
				if (varType.containsKey(ident)){
					String tt = varType.get(ident).getInternalType(); 
					if (tt.equals(jmlType.relT) || tt.equals(jmlType.setT)){
						translation = tmp + ident + ".equals(";
						predicate.getRight().accept(this);
						translation += ")";
					}else{
					translation = tmp + translation + " != ";
					predicate.getRight().accept(this);
					}
				}else{
					translation = tmp + translation + " != ";
					predicate.getRight().accept(this);
				}
				tryType = "";
			}
			break;
		case	Formula.LT:
			Print("LT");
			if (predTrans || assignmentTrans){
				predicate.getLeft().accept(this);
				translation += " < ";
				predicate.getRight().accept(this);
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case	Formula.LE:
			Print("LE");
			if (predTrans || assignmentTrans){
				predicate.getLeft().accept(this);
				translation += " <= ";
				predicate.getRight().accept(this);
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case	Formula.GT:
			Print("GT");
			if (predTrans || assignmentTrans){
				predicate.getLeft().accept(this);
				translation += " > ";
				predicate.getRight().accept(this);
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case	Formula.GE:
			Print("GE");
			if (predTrans || assignmentTrans){
				predicate.getLeft().accept(this);
				translation += " >= ";
				predicate.getRight().accept(this);
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.IN:
			Print("IN");
			if (predTrans){
				String tmp = translation;
				translation = "";
				varFuncRel.push("??");
				predicate.getRight().accept(this);
				if (translation.contains("??")){
					tmp = tmp + translation;
					translation = "";
					predicate.getLeft().accept(this);
					tmp = tmp.replace("??",translation);
					translation = tmp;
					varFuncRel.clear();
				}else{
					currentVar = translation;
					translation = tmp + translation + ".has(";
					predicate.getLeft().accept(this);
					translation += ")";
					varFuncRel.clear();
					currentVar = "";
				}
			}else if (boolExp){
				String tmp = translation;
				translation = "";
				predicate.getRight().accept(this);
				translation = tmp + translation + ".has(";
				predicate.getLeft().accept(this);
				translation += ")";
				Print("=====> " + translation);
				varFuncRel.clear();
				currentVar = "";
			}
			break;
		case 	Formula.NOTIN:
			Print("NOTIN");
			if (predTrans){
				String tmp = translation;
				translation = "";
				varFuncRel.push("??");
				predicate.getRight().accept(this);
				if (translation.contains("??")){
					tmp = tmp + "!" + translation;
					translation = "";
					predicate.getLeft().accept(this);
					tmp = tmp.replace("??",translation);
					translation = tmp;
					varFuncRel.clear();
				}else{
					currentVar = translation;
					translation = tmp + "!" + translation + ".has(";
					predicate.getLeft().accept(this);
					translation += ")";
					varFuncRel.clear();
					currentVar = "";
				}
			}
			break;
		case 	Formula.SUBSET:
			Print("SUBSET");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isProperSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = "";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.NOTSUBSET:
			Print("NOTSUBSET");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				translation += "!";
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isProperSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = "";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.SUBSETEQ:
			Print("SUBSETEQ");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = "";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.NOTSUBSETEQ:
			Print("NOTSUBSETEQ");
			if (predTrans || assignmentTrans){
				if (tryType.equals("")){
					tryType = jmlType.SET;
				}
				String tmp = translation;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				translation += "!";
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = "";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		}
	}

	@Override
	public void visitSimplePredicate(SimplePredicate predicate) {
		Print("\n visitSimplePredicate");
		switch (predicate.getTag()){
		case 	Formula.KFINITE:
			Print("KFINITE");
			String tmp = translation;
			translation = "";
			predicate.getExpression().accept(this);
			translation = tmp + translation + ".finite()";
			break;
		}
	}

	@Override
	public void visitUnaryPredicate(UnaryPredicate predicate) {
		Print("\n visitUnaryPredicate");
		//This is not predicate (check out the syntax) 
	}

	@Override
	public void visitExtendedExpression(ExtendedExpression expression) {
		Print("\n visitExtendedExpression");
		// source file: @noextend This class is not intended to be subclassed by clients.
	}

	@Override
	public void visitExtendedPredicate(ExtendedPredicate predicate) {
		Print("\n visitExtendedPredicate");
		// source file: @noextend This class is not intended to be subclassed by clients.
	}
	
	@Override
	public void visitPredicateVariable(PredicateVariable predVar) {
		Print("\n visitPredicateVariable");
		// source file: @noextend This class is not intended to be subclassed by clients.
	}

	public boolean as = false;
	public void Print(String s){
		if (as){
			System.out.println(s);
		}
	}
	
	public void Print(int s){
		if (as){
			System.out.println(s);
		}
	}
	
	public void PrintVarT(HashMap<String, ArrayList<String>> v){
		for (String var: v.keySet()){
			System.out.print(var + ": \n      ");
			for (String sv: v.get(var)){
				System.out.print(sv + "  -");
			}
			System.out.println();
			System.out.println();
			System.out.println();
		}
	}
	
	public void PrintVarT2(HashMap<String, jmlType> v){
		for (String var: v.keySet()){
			System.out.print(var + ": \n      ");
				System.out.println(v.get(var).getJmlType());
			}
		System.out.println();
		System.out.println();
		System.out.println();
	}
	
	public boolean isFunRel(int tag){
		return tag == Formula.REL ||
				tag == Formula.TREL ||
				tag == Formula.SREL ||
				tag == Formula.STREL ||
				tag == Formula.PFUN ||
				tag == Formula.TFUN ||
				tag == Formula.PINJ ||
				tag == Formula.TINJ ||
				tag == Formula.PSUR ||
				tag == Formula.TSUR ||
				tag == Formula.TBIJ; 
	}
	

}