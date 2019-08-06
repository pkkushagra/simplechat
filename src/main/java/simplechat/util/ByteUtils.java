package simplechat.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import simplechat.SimpleChatApplication;

import javax.servlet.ServletContext;
import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author masoud
 */
@Component
public class ByteUtils {

    @Autowired
    private ServletContext servletContext;

    private HashMap<String, Long> lastModified = new HashMap<>();
    private HashMap<String, byte[]> cachedBytes = new HashMap<>();

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] b = digest.digest(s.getBytes("UTF-8"));
            return toHex(b, "");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public String toHex(byte b[], String delimeter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String h = String.format("%h", b[i] & 0xff);
            if (h.length() == 1) {
                h = "0" + h;
            }
            sb.append((i == 0) ? h : (delimeter + h));
        }
        return sb.toString();
    }

    public String readString(File file) throws IOException {
        return new String(readBytes(file), "UTF-8");
    }

    public synchronized byte[] readBytes(File file) throws IOException {
        String path = file.getAbsolutePath();
        long lm = file.lastModified();
        if (cachedBytes.containsKey(path)) {
            if (lm == lastModified.get(path)) {
                return cachedBytes.get(path);
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(new FileInputStream(file), os, true, true);
        byte b[] = os.toByteArray();
        lastModified.put(path, lm);
        cachedBytes.put(path, b);
        return b;
    }

    public void copy(InputStream is, OutputStream os, boolean closeInput, boolean closeOutput) throws IOException {
        byte b[] = new byte[10000];
        while (true) {
            int r = is.read(b);
            if (r < 0) {
                break;
            }
            os.write(b, 0, r);
        }
        if (closeInput) {
            is.close();
        }
        if (closeOutput) {
            os.flush();
            os.close();
        }
    }

    public String serializeException(Exception ex) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintWriter writer = null;
            writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
            ex.printStackTrace(writer);
            writer.close();
            return new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "Unable to serialize";
        }
    }

    public String readPage(String fileName, Map<String, String> params) throws IOException {
        String page = readString(new File(SimpleChatApplication.pageResourcePath + fileName));
        int si = 0, ei = -2;
        StringBuilder sb = new StringBuilder();
        while (true) {
            int oei = ei;
            si = page.indexOf("<%", ei + 2);
            if (si < 0) {
                break;
            }
            ei = page.indexOf("%>", si);
            String cmd = page.substring(si + 2, ei).trim();
            String rep;
            switch (cmd.charAt(0)) {
                case '$':
                    rep = params.get(cmd.substring(1));
                    break;
                case '#':
                    rep = readPage("/" + cmd.substring(1), params);
                    break;
                default:
                    throw new IOException("Invalid identifier");
            }
            sb.append(page, oei + 2, si);
            sb.append(rep);
        }
        sb.append(page, ei + 2, page.length());
        return sb.toString();
    }

    public String humanReadableSize(long len) {
        String lstr = "Too Big";
        if (len < 1000) {
            lstr = len + " B";
        } else if (len < 1000000) {
            lstr = len / 1000.0 + " KB";
        } else if (len < 1000000000) {
            lstr = (len / 1000) / 1000.0 + " MB";
        } else if (len < 1000000000000L) {
            lstr = (len / 1000000) / 1000.0 + " GB";
        } else if (len < 1000000000000000L) {
            lstr = (len / 1000000000L) / 1000.0 + " TB";
        } else if (len < 1000000000000000000L) {
            lstr = (len / 1000000000000L) / 1000.0 + " PB";
        }
        return lstr;
    }

}
