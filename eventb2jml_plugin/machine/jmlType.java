package eventb2jml_plugin.machine;

public class jmlType {
	
	static final String boolT = "Boolean";
	static final String intT = "Integer";
	static final String setT = "BSet";
	static final String relT = "BRel";
	
	static final String NATIVE = "NATIVE";
	static final String SET = "SET";
	
	
	private String name;
	private String internalType; // could be either boolT or intT or setT or relT
	
	//variables used in case the var is a BRelation
	private jmlType domain;
	private jmlType range;
	
	//variables used in case the var is a BSet
	private jmlType setType;
	
	public boolean d;
	
	
	jmlType(){
		d = true;
	}
	
	jmlType(String Name, String jmlType){
		d = false;
		name = Name;
		internalType = jmlType;
	}
	
	public String getInternalType(){
		return internalType;
	}
	
	public void setValues(String n, String t){
		d = false;
		name = n;
		internalType = t;
	}
	
	public jmlType getSetType(){
		return setType;
	}
	
	public String getName(){
		return name;
	}
	
	public String getJmlType(){
		if (internalType.equals(boolT) ||
				internalType.equals(intT)){
			return internalType;
			
		}
		return getType2(this,"");
	}
	
	private String getType2(jmlType t, String ty){
		if (t.internalType.equals(relT)){
			ty += "BRelation<";
			ty = getType2(t.getDomainType(),ty);
			ty += ",";
			ty = getType2(t.getRangeType(),ty);
			ty += ">";
		}else if (t.internalType.equals(setT)){
			ty += "BSet<";
			ty = getType2(t.getSetType(),ty);
			ty += ">";
		}else
			ty += t.internalType;
		return ty;
	}
	
	public boolean update(String t){
		d = false;
		if (internalType.equals(relT)){
			return updateRel(t);
		}
		if (internalType.equals(setT)){
			return updateSet(t);
		}
		return false;
	}
	
	private boolean updateSet(String t){
		if (setType == null){
			setType = new jmlType("", t);
			return true;
		}else{
			if (setType.getInternalType().equals(setT)){
				return setType.updateSet(t);
			}else if (setType.getInternalType().equals(relT)){
				return setType.updateRel(t);
			}
		}
		return false;
	}
	
	//uses for the creation of either BRelation or BSet
	private boolean updateRel(String t){
		if (domain == null){
			//it is necessary to create a internalType
			domain = new jmlType("",t);
			return true;
		}
		
		if (domain.internalType.equals(relT)){
			if (domain.updateRel(t)){
				return true;
			}
		}
			//already done with the domain.. try with the range
			if (range == null){
				//it is necessary to create a internalType
				range = new jmlType("",t);
				return true;
			}
			
			if (range.internalType.equals(relT)){
				if (range.updateRel(t)){
					return true;
				}
			}
			
		return false;
	}
	
	
	//methods for BRelations
	public jmlType getDomainType(){
		return domain;
	}
	
	public jmlType getRangeType(){
		return range;
	}
	
	public static void main(String[] args){
		jmlType t = new jmlType("zz","Integer");
		System.out.println(t.getJmlType());
	}

}
