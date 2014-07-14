/*
 * THIS CLASS CONTAINS ALL THE MATRIX FUNCTION NEEDED FOR THE APLICATION 
 * WHICH IS NOT BUILT IN THE SUPPORTED LIBRARY 
 * NO NEED TO CHANGE.
 */
package com.example.deadreckoning;

public class MatrixHelper {
	
	public static double[] convertFloatsToDoubles(float[] input)
	{
	    if (input == null)
	    {
	        return null; // Or throw an exception - your choice
	    }
	    double[] output = new double[input.length];
	    for (int i = 0; i < input.length; i++)
	    {
	        output[i] = input[i];
	    }
	    return output;
	}
	
	public static double[][] matrixFrom1DArray(float[] arrayIn, int rows, int cols) {
		return MatrixHelper.matrixFrom1DArray(MatrixHelper.convertFloatsToDoubles(arrayIn), rows, cols);
	}
	
	public static double[][] matrixFrom1DArray(double[] arrayIn, int rows, int cols) {
		if(rows*cols>arrayIn.length) {
			throw new IllegalArgumentException("MatrixHelper::matrixFrom1DArray Can't create matrice of the requested size! (Requestec "+rows+"x"+cols+", array size: "+arrayIn.length+")");
		}
		double[][] matrixOut = new double[rows][cols];
		int k =0;
		for(int i=0;i<rows;i++) {
			for(int j=0;j<cols;j++) {
				matrixOut[i][j]=arrayIn[k];
				k++;
			}
		}
		return matrixOut;
	}
	
	public static double[][] matrixMultiply(double[] m1in, int rows1, int cols1, double[] m2in, int rows2, int cols2) {
		double[][] m1 = MatrixHelper.matrixFrom1DArray(m1in, rows1, cols1);
		double[][] m2 = MatrixHelper.matrixFrom1DArray(m2in, rows2, cols2);
		return MatrixHelper.matrixMultiply(m1, m2);
	}
	
	public static double[][] matrixMultiply(float[] m1in, int rows1, int cols1, float[] m2in, int rows2, int cols2) {
		double[][] m1 = MatrixHelper.matrixFrom1DArray(m1in, rows1, cols1);
		double[][] m2 = MatrixHelper.matrixFrom1DArray(m2in, rows2, cols2);
		return MatrixHelper.matrixMultiply(m1, m2);
	}
	
	public static double[][] matrixMultiply(double[][] m1, double[][] m2) {
	    int m1rows = m1.length;
	    int m1cols = m1[0].length;
	    int m2rows = m2.length;
	    int m2cols = m2[0].length;
	    if (m1cols != m2rows)
	      throw new IllegalArgumentException("matrices don't match: " + m1cols + " != " + m2rows);
	    double[][] result = new double[m1rows][m2cols];

	    // multiply
	    for (int i=0; i<m1rows; i++)
	      for (int j=0; j<m2cols; j++)
	        for (int k=0; k<m1cols; k++)
	        	result[i][j] += m1[i][k] * m2[k][j];

	    return result;
	  }
	
	public static float[] arrayMultiply33(float[] A, float[] B) {
        float[] result = new float[9];
	     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
	
	public static double floattodouble(float fvar){
		return Double.parseDouble(new Float(fvar).toString());
	}
}
