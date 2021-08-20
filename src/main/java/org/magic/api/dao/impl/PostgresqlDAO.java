package org.magic.api.dao.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.magic.api.beans.Grading;
import org.magic.api.beans.MagicCard;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.api.interfaces.abstracts.AbstractMagicSQLDAO;
import org.postgresql.util.PGobject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PostgresqlDAO extends AbstractMagicSQLDAO {

	private static final String URL_PGDUMP = "URL_PGDUMP";

	@Override
	protected String getAutoIncrementKeyWord() {
		return "SERIAL";
	}

	
	@Override
	protected String getjdbcnamedb() {
		return "postgresql";
	}

	@Override
	protected String beanStorage() {
		return "json";
	}


	@Override
	protected String longTextStorage() {
		return "TEXT";
	}
	
	@Override
	protected String createListStockSQL() {
		return "SELECT * FROM  stocks WHERE mcard->>'name' = ? and collection = ?";
	}
	
	@Override
	protected void storeCard(PreparedStatement pst, int position, MagicCard mc) throws SQLException {
		var jsonObject = new PGobject();
		jsonObject.setType("json");
		jsonObject.setValue(serialiser.toJsonElement(mc).toString());
		pst.setObject(position, jsonObject);
	}
	
	
	@Override
	protected void storeTransactionItems(PreparedStatement pst, int position, List<MTGStockItem> grd) throws SQLException {
		var jsonObject = new PGobject();
		jsonObject.setType("json");
		jsonObject.setValue(serialiser.toJsonElement(grd).toString());
		pst.setObject(position,jsonObject );
		
	}
	
	@Override
	protected void storeGrade(PreparedStatement pst, int position, Grading grd) throws SQLException {
		var jsonObject = new PGobject();
		jsonObject.setType("json");
		jsonObject.setValue(serialiser.toJsonElement(grd).toString());
		pst.setObject(position, jsonObject);
	}
	
	
	@Override
	protected void storeTiersApps(PreparedStatement pst, int i, Map<String, String> tiersAppIds) throws SQLException {
		
		var jsonObject = new PGobject();
		jsonObject.setType("json");
		jsonObject.setValue(serialiser.toJsonElement(tiersAppIds).toString());
		pst.setObject(i, jsonObject);
	}
	
	@Override
	protected void storeDeckBoard(PreparedStatement pst, int i, Map<MagicCard, Integer> board) throws SQLException {
		var jsonObject = new PGobject();
		jsonObject.setType("json");
		var arr = new JsonArray();
		board.entrySet().forEach(e->{
			var obj = new JsonObject();
			obj.addProperty("qty", e.getValue());
			obj.add("card", serialiser.toJsonElement(e.getKey()));
			arr.add(obj);
		});
		jsonObject.setValue(arr.toString());
		
		pst.setObject(i, jsonObject);
	}
	
	
	
	@Override
	protected String getdbSizeQuery() {
		return "SELECT pg_database_size('"+getString(DB_NAME)+"');";
	}

	@Override
	public long getDBSize() {
		try (var c = pool.getConnection(); PreparedStatement pst = c.prepareStatement(getdbSizeQuery(),ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY,ResultSet.HOLD_CURSORS_OVER_COMMIT); ResultSet rs = pst.executeQuery();) {
			rs.first();
			return (long) rs.getDouble(1);
		} catch (SQLException e) {
			logger.error(e);
			return 0;
		}
	}

	@Override
	public String getName() {
		return "PostGreSQL";
	}

	@Override
	public void backup(File f) throws IOException {

		if (getString(URL_PGDUMP).length() <= 0) {
			throw new NullPointerException("Please fill URL_PGDUMP var");
		}

		String dumpCommand = getString(URL_PGDUMP) + "/pg_dump" + " -d" + getString(DB_NAME)
				+ " -h" + getString(SERVERNAME) + " -U" + getString(LOGIN) + " -p"
				+ getString(SERVERPORT);

		var rt = Runtime.getRuntime();
		logger.info("begin Backup :" + dumpCommand);

		Process child = rt.exec(dumpCommand);
		try (var ps = new PrintStream(f)) {
			var in = child.getInputStream();
			int ch;
			while ((ch = in.read()) != -1) {
				ps.write(ch);
			}
			logger.info("Backup " + getString(DB_NAME) + " done");
		}

	}

	
	@Override
	public Map<String, String> getDefaultAttributes() {
		return Map.of(SERVERPORT, "5432",
				   LOGIN, "postgres",
				   PASS, "postgres",
				   PARAMS, "",
				   URL_PGDUMP, "C:/Program Files (x86)/PostgreSQL/9.5/bin"
				);
	}
	

}
