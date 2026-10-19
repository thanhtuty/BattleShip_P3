package battleship.part3.server;
public final class Ship {
 private final int id, length; private int hits;
 Ship(int id,int length){this.id=id;this.length=length;}
 public int id(){return id;} public int length(){return length;} public int hits(){return hits;}
 void hit(){if(hits<length)hits++;} public boolean sunk(){return hits==length;}
}
