package applemailfix;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class BlobLoader {
	
	public static String inputStreamToString(InputStream is) throws IOException {
	    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	        return br.lines().collect(Collectors.joining(System.lineSeparator()));
	    }
	}

	public byte [] loadBlobByteArr(String name) {
        ByteArrayOutputStream bos = null;
        try {
            InputStream input = BlobLoader.class.getResourceAsStream("/"+name);
            byte[] buffer = new byte[1024];
            bos = new ByteArrayOutputStream();
            for (int len; (len = input.read(buffer)) != -1;) {
                bos.write(buffer, 0, len);
            }
            input.close();
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e2) {
            System.err.println(e2.getMessage());
        }
        return bos != null ? bos.toByteArray() : null;
	}	
}
