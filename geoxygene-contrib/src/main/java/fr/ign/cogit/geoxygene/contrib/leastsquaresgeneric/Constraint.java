package fr.ign.cogit.geoxygene.contrib.leastsquaresgeneric;


//=======================================================================
// Class to handle constraints in mathematical expressions
// Date : 30/03/2015
// Contact : yann.meneroux@ign.fr
//=======================================================================

public class Constraint {

	// Type of constrait
	private String type;

	// Mathematical expression
	private String expression;

	// right part (numerical)
	private double rightPart;

	// left part (literal)
	private String leftPart;

	// Reverse Polish Notation
	private ReversePolishNotation rpn;

	// Weights
	private double weight = 1;
	private double variance = 1;
	private double stddev = 1;

	// Getters
	public String getType(){return type;}
	public String getExpression(){return expression;}
	public ReversePolishNotation getReversePolishNotation(){return rpn;}
	public double getRightPart(){return rightPart;}
	public String getLeftPart(){return leftPart;}
	public double getWeight(){return weight;}
	public double getVariance(){return variance;}
	public double getStddev(){return stddev;}

	// Setters
	public void setType(String type){this.type = type;}
	public void setExpression(String expression){this.expression = expression;}

	// Set weight systems

	public void setWeight(double weight){

		this.weight = weight;
		this.stddev = 1/weight;
		this.variance = stddev*stddev;

	}

	public void setStddev(double stddev){

		this.stddev = stddev;
		this.weight = 1/stddev;
		this.variance = stddev*stddev;

	}

	public void setVariance(double variance){

		this.variance = variance;
		this.stddev = Math.sqrt(variance);
		this.weight = 1/stddev;

	}

	// -------------------------------------------------------------------
	// General method to build a constraint
	// Input : mathematical expression (string) and type (string)
	// -------------------------------------------------------------------
	public Constraint(String expression, String type){

		this.expression = expression;
		this.type = type;

		split();

		this.rpn = new ReversePolishNotation(leftPart);

	}

	// -------------------------------------------------------------------
	// Method to build a constraint
	// Input : mathematical expression (string)
	// -------------------------------------------------------------------
	public Constraint(String expression){

		this(expression, "generic");

	}

	// -------------------------------------------------------------------
	// ToString method redefinition
	// Output : constraint in expressive form
	// -------------------------------------------------------------------
	public String toString(){

		return this.expression;

	}

	// -------------------------------------------------------------------
	// Method to split the constraint in right part and left part
	// -------------------------------------------------------------------
	private void split(){

		// String copy
		String exp = new String(expression);

		// Supressing spaces in string
		exp = exp.replaceAll("\\s+","");

		// Splitting
		int posEqual = exp.indexOf('=');

		if (posEqual == -1){

			System.out.println("Error : constraint ["+expression+"] should be an equation");
			System.exit(0);

		}

		this.leftPart = exp.substring(0,posEqual);

		String temp = exp.substring(posEqual+1, exp.length());

		if (!ExpressionComputer.isNumeric(temp)){

			System.out.println("Error in constraint ["+expression+"] : right part should be numeric");
			System.exit(0);

		}

		this.rightPart = Double.parseDouble(temp);

	}

}
