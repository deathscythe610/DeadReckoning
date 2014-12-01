package com.example.deadreckoning;


public class SensorFilters {
	
	public SensorFilters(){
		
	}
	
	//Kalman variables
	double KalmanProcessNoiseVar = 0.25;
	double KalmanSensorNoiseVar = 2;
	double KalmanEstimatedValue = 0.5;
	double KalmanEstimatedErrorVar = 0.4;
	double KalmanGain = 0.4;
	//Moving Avarage variable
	static int samplenumber = 2;
	int samplingIndex = 0;
	public double[] sampleSlots = new double[samplenumber]; 
	boolean startUpdate = false;
	//Low Pass Filter variables 
	double prevAccVal = 0; 
	double LPFConstant = 0.95;
	
	
//********************************************************************************************************************************//
//													KALMAN FILTER 1 DIMENSION 
//********************************************************************************************************************************//
	
	
	
	public double kalman_update(double measurement){
		//Prediction update 
		//Omit x = x
		this.KalmanEstimatedErrorVar = this.KalmanEstimatedErrorVar + this.KalmanProcessNoiseVar;
		
		//Measurement update
		this.KalmanGain = this.KalmanEstimatedErrorVar/(this.KalmanEstimatedErrorVar + this.KalmanSensorNoiseVar);
		this.KalmanEstimatedValue = this.KalmanEstimatedValue + this.KalmanGain*(measurement - this.KalmanEstimatedValue);
		this.KalmanEstimatedErrorVar = (1 - this.KalmanGain)*this.KalmanEstimatedErrorVar;
		//Return new value
		return this.KalmanEstimatedValue;
	}


//********************************************************************************************************************************//
//													MOIVNG AVARAGE FILTER (3 HISTORY DATA) 
//********************************************************************************************************************************//	
		
	public void input__movingAvarage_filter(double measurement){
		if ((!startUpdate) && (samplingIndex==samplenumber-1)) startUpdate = true; 
		if (samplingIndex>=samplenumber){
			samplingIndex = 0;
		}
		sampleSlots[samplingIndex] = measurement;
		samplingIndex++;
	}
		
	public double movingAvarage_update(){
		if(startUpdate){
			int sum = 0;
			for (int k=0; k<samplenumber; k++){
				sum += sampleSlots[k];
			}
			return sum/samplenumber;
		}
		else 
			return sampleSlots[samplingIndex -1];
	}
	
//********************************************************************************************************************************//
//												LOW PASS FILTER (1ST ORDER) 
//********************************************************************************************************************************//	
	public double LowPassFilter_update(double measurement){
		if (prevAccVal==0){
			prevAccVal = measurement; 
			return measurement;
		}
		else{ 
			prevAccVal = measurement*LPFConstant + prevAccVal*(1-LPFConstant);
			return prevAccVal;
		}
	}
}