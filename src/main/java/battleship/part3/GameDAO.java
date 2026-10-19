package battleship.part3;
import java.sql.*;
public final class GameDAO {
 private final DbConfig db; public GameDAO(DbConfig db){this.db=db;}
 public long create(String a,String b)throws SQLException{try(Connection c=db.connect()){c.setAutoCommit(false);try(PreparedStatement p=c.prepareStatement("INSERT dbo.Matches(Player1,Player2,Status) OUTPUT INSERTED.MatchId VALUES(?,?,'SETUP')")){p.setString(1,a);p.setString(2,b);try(ResultSet r=p.executeQuery()){r.next();long id=r.getLong(1);try(PreparedStatement q=c.prepareStatement("INSERT dbo.PlayerBoards(MatchId,PlayerName) VALUES(?,?),(?,?)")){q.setLong(1,id);q.setString(2,a);q.setLong(3,id);q.setString(4,b);q.executeUpdate();}c.commit();return id;}}catch(SQLException e){c.rollback();throw e;}}}
 public void event(long id,String player,String type,String details)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("INSERT dbo.GameEvents(MatchId,PlayerName,EventType,Details) VALUES(?,?,?,?)")){p.setLong(1,id);p.setString(2,player);p.setString(3,type);p.setString(4,details);p.executeUpdate();}}
 public void ready(long id,String player,String board)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE dbo.PlayerBoards SET BoardState=?,Ready=1 WHERE MatchId=? AND PlayerName=?")){p.setString(1,board);p.setLong(2,id);p.setString(3,player);p.executeUpdate();}}
 public void start(long id,String active)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE dbo.Matches SET Status='PLAYING',ActivePlayer=?,StartedAt=SYSUTCDATETIME() WHERE MatchId=?")){p.setString(1,active);p.setLong(2,id);p.executeUpdate();}}
 public void shot(long id,String shooter,String target,String result,int r,int c,String next,String board)throws SQLException{try(Connection cn=db.connect()){cn.setAutoCommit(false);try(PreparedStatement e=cn.prepareStatement("INSERT dbo.GameEvents(MatchId,PlayerName,EventType,Details) VALUES(?,?,?,?)");PreparedStatement b=cn.prepareStatement("UPDATE dbo.PlayerBoards SET BoardState=?,ShotsTaken=ShotsTaken+1 WHERE MatchId=? AND PlayerName=?");PreparedStatement m=cn.prepareStatement("UPDATE dbo.Matches SET ActivePlayer=? WHERE MatchId=?")){e.setLong(1,id);e.setString(2,shooter);e.setString(3,result);e.setString(4,"Tọa độ "+(r+1)+","+(c+1)+"; mục tiêu="+target);e.executeUpdate();b.setString(1,board);b.setLong(2,id);b.setString(3,target);b.executeUpdate();m.setString(1,next);m.setLong(2,id);m.executeUpdate();cn.commit();}catch(SQLException e){cn.rollback();throw e;}}}
 public void changeTurn(long id,String active)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE dbo.Matches SET ActivePlayer=? WHERE MatchId=?")){p.setString(1,active);p.setLong(2,id);p.executeUpdate();}}
 public void finish(long id,String winner)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE dbo.Matches SET Status='FINISHED',Winner=?,FinishedAt=SYSUTCDATETIME() WHERE MatchId=?")){p.setString(1,winner);p.setLong(2,id);p.executeUpdate();}}
 public String history(String username)throws SQLException{
  StringBuilder text=new StringBuilder("LỊCH SỬ 10 TRẬN GẦN NHẤT CỦA ").append(username).append("\n\n");
  try(Connection c=db.connect();PreparedStatement p=c.prepareStatement(
      "SELECT TOP (10) MatchId,Player1,Player2,Status,Winner,CreatedAt FROM dbo.Matches WHERE Player1=? OR Player2=? ORDER BY MatchId DESC")){
   p.setString(1,username);p.setString(2,username);
   try(ResultSet r=p.executeQuery()){
    boolean found=false;
    while(r.next()){
     found=true;long id=r.getLong("MatchId");
     String other=username.equals(r.getString("Player1"))?r.getString("Player2"):r.getString("Player1");
     text.append("Trận #").append(id).append(" với ").append(other)
       .append(" | ").append(r.getString("Status"))
       .append(" | Thắng: ").append(r.getString("Winner")==null?"chưa có":r.getString("Winner"))
       .append(" | ").append(r.getTimestamp("CreatedAt")).append("\n");
     try(PreparedStatement e=c.prepareStatement(
         "SELECT TOP (40) EventType,Details,CreatedAt FROM dbo.GameEvents WHERE MatchId=? ORDER BY EventId")){
      e.setLong(1,id);
      try(ResultSet events=e.executeQuery()){
       while(events.next())text.append("   • ").append(events.getString("EventType"))
         .append(": ").append(events.getString("Details")==null?"":events.getString("Details"))
         .append(" (").append(events.getTimestamp("CreatedAt")).append(")\n");
      }
     }
     text.append("\n");
    }
    if(!found)text.append("Bạn chưa có trận đấu nào được lưu.");
   }
  }
  return text.toString();
 }
}
