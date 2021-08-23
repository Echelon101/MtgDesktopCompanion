package org.magic.api.dav;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.attribute.FileAttribute;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGDao;
import org.magic.services.CardsManagerService;
import org.magic.services.MTGLogger;

public class MTGByteChannel implements SeekableByteChannel {

	private long position;
	protected Logger logger = MTGLogger.getLogger(this.getClass());
	private MTGPath path;
	private MTGDao dao;
	private byte[] content;
	private boolean open;
	
	public MTGByteChannel(MTGPath path, MTGDao dao, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) {
		
		logger.debug("new ByteChannel for " + path + " opts : " + options + " " + ArrayUtils.toString(attrs));
		
		this.path=path;
		this.dao=dao;
		content = new byte[0];
		open=true;
	}

	@Override
	public void close() throws IOException {
		open=false;

	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public long position() throws IOException {
		return position;
	}

	@Override
	public SeekableByteChannel position(long newp) throws IOException {
		if(!isOpen())
			throw new IOException("Channel is closed");
		
		this.position = newp;
		return this;
	}

	
	@Override
	public int read(ByteBuffer dst) throws IOException {
		
		try {
			Optional<MagicCard> card = dao.listCardsFromCollection(path.getCollection(), new MagicEdition(path.getIDEdition())).stream().filter(mc->mc.getName().equals(path.getCardName())).findFirst();
			if(card.isPresent())
				content = ((MTGFileSystem)path.getFileSystem()).getSerializer().toJsonElement(card.get()).toString().getBytes();
		} catch (Exception e) {
			logger.error(e);
		}
		
		if (position > size()) {
            position = size();
        }
		
		int wanted = dst.remaining();
        int possible = (int) (size() - position);
        if (possible <= 0) {
            return -1;
        }
       
        if (wanted > possible) {
            wanted = possible;
        }
        dst.put(content, (int)position, wanted);
        position += wanted;
        return wanted;
	}

	@Override
	public long size() throws IOException {
		return content.length;
	}

	@Override
	public SeekableByteChannel truncate(long arg0) throws IOException {
		throw new IOException("truncate() not implemented");
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		var len = src.remaining();
		var buf = new byte[len];
	    while (src.hasRemaining()) {
	      src.get(buf);
	    }
	    
	    MagicCard mc = ((MTGFileSystem)path.getFileSystem()).getSerializer().fromJson(new String(buf) , MagicCard.class);
		
		try {
			CardsManagerService.saveCard(mc, path.getCollection(), null);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	    return len;
	}

}
