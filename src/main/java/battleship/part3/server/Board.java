package battleship.part3.server;
import java.util.*;
public final class Board {
 public static final int SIZE=6; public static final int[] FLEET={2,3,4,5};
 private final int[][] shipAt=new int[SIZE][SIZE]; private final boolean[][] fired=new boolean[SIZE][SIZE];
 private final Ship[] ships=new Ship[FLEET.length]; private int placed;
 public Board(){for(int i=0;i<FLEET.length;i++)ships[i]=new Ship(i+1,FLEET[i]);}
 public int placed(){return placed;} public int nextLength(){return placed<FLEET.length?FLEET[placed]:0;} public boolean ready(){return placed==FLEET.length;}
 public void place(int row,int col,boolean horizontal){if(ready())throw new IllegalStateException("Đã đặt đủ 4 tàu"); int len=nextLength();
  for(int k=0;k<len;k++){int r=row+(horizontal?0:k),c=col+(horizontal?k:0);if(r<0||r>=SIZE||c<0||c>=SIZE)throw new IllegalArgumentException("Tàu vượt khỏi bàn 6×6");if(shipAt[r][c]!=0)throw new IllegalArgumentException("Tàu bị chồng ô");}
  for(int k=0;k<len;k++)shipAt[row+(horizontal?0:k)][col+(horizontal?k:0)]=placed+1;placed++;}
 public String shoot(int r,int c){check(r,c);if(fired[r][c])throw new IllegalArgumentException("Ô này đã bắn");fired[r][c]=true;int id=shipAt[r][c];if(id==0)return "MISS";ships[id-1].hit();return ships[id-1].sunk()?"SUNK":"HIT";}
 public Cell cellAt(int row,int col,boolean owner){check(row,col);if(fired[row][col])return shipAt[row][col]==0?Cell.MISS:Cell.HIT;return owner&&shipAt[row][col]!=0?Cell.SHIP:Cell.WATER;}
 public boolean allSunk(){return ready()&&Arrays.stream(ships).allMatch(Ship::sunk);}
 private void check(int r,int c){if(r<0||r>=SIZE||c<0||c>=SIZE)throw new IllegalArgumentException("Tọa độ ngoài bàn cờ");}
 public String encode(){return encode(true);}
 public String encode(boolean owner){StringBuilder s=new StringBuilder(36);for(int r=0;r<SIZE;r++)for(int c=0;c<SIZE;c++)s.append(fired[r][c]?(shipAt[r][c]==0?'2':'3'):(owner&&shipAt[r][c]!=0?'1':'0'));return s.toString();}
 public static Board parse(String data){String[] ps=data.split(";",-1);if(ps.length!=4)throw new IllegalArgumentException("Cần đặt đủ 4 tàu");Board b=new Board();for(String p:ps){String[] x=p.split(",");if(x.length!=3||(!x[2].equals("H")&&!x[2].equals("V")))throw new IllegalArgumentException("Sai dữ liệu tàu");b.place(Integer.parseInt(x[0]),Integer.parseInt(x[1]),x[2].equals("H"));}return b;}
 public static String randomSetup(){Random q=new Random();Board b=new Board();List<String> a=new ArrayList<>();while(!b.ready()){int r=q.nextInt(6),c=q.nextInt(6);boolean h=q.nextBoolean();try{b.place(r,c,h);a.add(r+","+c+","+(h?"H":"V"));}catch(IllegalArgumentException ignored){}}return String.join(";",a);}
}
