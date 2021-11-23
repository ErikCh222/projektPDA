package sample;

public class RobotFunction {
	private int parameter;
	private String functionName;
	
	public RobotFunction(int param, String name) {
		this.parameter = param;
		this.functionName = name;
	}
	
	public int getParameter() {
		return parameter;
	}
	public void setParameter(int parameter) {
		this.parameter = parameter;
	}
	public String getFunctionName () {
		return functionName;
	}
	public void setFunctionName(Runnable function) {
		this.functionName = functionName;
	}
	
}
