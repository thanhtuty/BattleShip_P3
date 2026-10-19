package battleship.part3;
public final class TurnManager {
 private int current; private long deadline;
 public TurnManager(int first){current=first;reset();} public int current(){return current;} public int secondsLeft(){return (int)Math.max(0,(deadline-System.currentTimeMillis()+999)/1000);}
 public void hit(){reset();} public void miss(){current=1-current;reset();} public boolean expired(){return System.currentTimeMillis()>=deadline;} private void reset(){deadline=System.currentTimeMillis()+15_000;}
}
