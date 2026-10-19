package battleship.part3;
/** Chạy thủ công qua NetBeans Run File để kiểm tra luật cơ bản, không cần SQL Server. */
public final class SelfTest {
 public static void main(String[] args){Board b=Board.parse("0,0,H;1,0,H;2,0,H;3,0,H");if(!b.ready())throw new AssertionError();try{Board.parse("0,0,H;0,1,H;2,0,H;3,0,H");throw new AssertionError("Cho phép tàu chồng");}catch(IllegalArgumentException expected){}
  Board c=Board.parse("0,0,H;1,0,H;2,0,H;3,0,H");if(!c.shoot(5,5).equals("MISS"))throw new AssertionError();if(!c.shoot(0,0).equals("HIT")||!c.shoot(0,1).equals("SUNK"))throw new AssertionError();TurnManager t=new TurnManager(0);t.hit();if(t.current()!=0)throw new AssertionError("Trúng phải giữ lượt");t.miss();if(t.current()!=1)throw new AssertionError("Trượt phải đổi lượt");System.out.println("SelfTest OK");}
}
