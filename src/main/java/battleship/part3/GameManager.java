package battleship.part3;
/** Bộ điều phối luật dùng được độc lập trong kiểm thử hoặc lớp mạng. */
public final class GameManager {private final Game game=new Game();public Game game(){return game;}public void place(int player,int r,int c,boolean horizontal){game.board(player).place(r,c,horizontal);}public void ready(int player){game.ready(player);}public String attack(int player,int r,int c){return game.attack(player,r,c);}}
