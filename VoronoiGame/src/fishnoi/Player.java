package fishnoi;

public class Player {
	private String name;
	private double score;
	
	public Player(String name){
		this.name = name;
		this.score = 0;
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public double getScore(){
		return score;
	}
	
	public void setScore(double score){
		this.score = score;
	}
	
	public void updateScore(double addedScore){
		this.score += addedScore;
	}
	
	public void resetScore(){
		this.score = 0;
	}
}
