package battleship.part3.server;
/** Luật trận: trúng/sunk giữ lượt; miss đổi lượt; hết thời gian 15 giây cũng đổi lượt. */
public final class Game {
 private final Board[] boards={new Board(),new Board()}; private TurnManager turns; private boolean started,finished;
 public Board board(int player){return boards[player];} public void ready(int player){if(!boards[player].ready())throw new IllegalStateException("Chưa đặt đủ tàu");if(boards[0].ready()&&boards[1].ready()){started=true;turns=new TurnManager(0);}}
 public String attack(int player,int row,int col){if(!started||finished)throw new IllegalStateException("Trận chưa sẵn sàng");if(turns.current()!=player)throw new IllegalStateException("Chưa tới lượt");String result=boards[1-player].shoot(row,col);if(result.equals("MISS"))turns.miss();else turns.hit();finished=boards[1-player].allSunk();return result;}
 public TurnManager turns(){return turns;} public boolean finished(){return finished;}
}
