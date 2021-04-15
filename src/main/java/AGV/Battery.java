package AGV;

public class Battery {
	public final double maxCap;
	public double currentCap;
	
	public Battery(double maxCap) {
		this.maxCap=maxCap;
		this.currentCap=0;
	}
	
	public double getCharge() {
		return this.currentCap;
	}
	
	public boolean fullyCharged() {
		return currentCap==maxCap;
	}
	
	public int  getRemainingCharge() {
		return (int) Math.round(currentCap/maxCap *100);
	}
	
	public void increaseCharge(double charge) {
		currentCap+=charge;
		if(currentCap>maxCap)
			currentCap=maxCap;
	}
	
	public void decreaseCharge(double usedCharge) {
		currentCap-=usedCharge;
		if(currentCap<0)
			currentCap=0;
	}

	public boolean isEmpty() {
		return currentCap<=0;
	}
	
	public double batteryLifeAfterDistance(double potentialDistance) {
		return (currentCap-potentialDistance)/maxCap;
	}

	public double getRange() {
		return this.currentCap;
	}
	
}
