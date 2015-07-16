import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * calculates difference between the geoid (egm96) and the (wgs84) altitude provided by gps receivers.
 * example use case: android only provides the wgs84 altitudes, what normal people expect is the (egm96)
 *  
 * link: http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/egm96.html
 * cpp source: http://geographiclib.sourceforge.net/1.28/Geoid_8cpp_source.html
 * 
 * http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/outintpt.dat
 * check with test data:
 *   38.628155  269.779155 -31.628
 *  -14.621217  305.021114  -2.969
 *   46.874319  102.448729 -43.575
 *  -23.617446  133.874712  15.871
 *   38.625473  359.999500  50.066
 *  -00.466744    0.002300  17.329
 *  
 *  uses the cgi script:
 *  http://earth-info.nga.mil/nga-bin/gandg-bin/intpt.cgi
 *  which is used in:
 *  http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/intpt.html?
 *  
 * @author matthiaszimmermann
 *
 */
public class GeoidCalculator {
	
	public static final String CGI = "http://earth-info.nga.mil/nga-bin/gandg-bin/intpt.cgi";
	public static final String UNITS = "Units=meters";
	public static final String HEIGHT_PATTERN = "<br>([^<]+) Meters<br>";
	
	public static void main(String args[]) {
		if(args.length == 2) {
			double lat = Double.parseDouble(args[0]);
			double lng = Double.parseDouble(args[1]);
			
			System.out.println(getHeight(lat, lng));
		}
	}
	
	private static double getHeight(double lat, double lng) {
		StringBuffer html = new StringBuffer();
		
		try {
			URL url = new URL(CGI);
			URLConnection conn = url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(createParams(lat, lng));
			out.flush();
			out.close();

			DataInputStream in = new DataInputStream(conn.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;

			while((line = reader.readLine()) != null) {
				html.append(line);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	    
	    return parseCgiOutput(html.toString());
	}

	private static String createParams(double lat, double lng) {
		StringBuffer params = new StringBuffer();
	    params.append("LatitudeDeg=");  params.append(d2d(lat)); params.append("&");
	    params.append("LatitudeMin=");  params.append(d2m(lat)); params.append("&");
	    params.append("LatitudeSec=");  params.append(d2s(lat)); params.append("&");
	    params.append("LongitudeDeg="); params.append(d2d(lng)); params.append("&");
	    params.append("LongitudeMin="); params.append(d2m(lng)); params.append("&");
	    params.append("LongitudeSec="); params.append(d2s(lng)); params.append("&");
	    params.append(UNITS);
	    
		return params.toString();
	}
	
	private static double parseCgiOutput(String html) {
	    Pattern pattern = Pattern.compile(HEIGHT_PATTERN, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(html);
	    
	    if(matcher.find()) {
	    	return Double.parseDouble(matcher.group(1).trim());
	    }
	    
	    return -9999.99;
	}
	
	private static String d2d(double d) {
		return Integer.toString((int)(Math.signum(d) * degToDeg(d)));
	}
	
	private static String d2m(double d) {
		return Integer.toString(d2sign(d) * degToMin(d));
	}
	
	private static String d2s(double d) {
		return d2s11(d2sign(d) * degToSec(d));
	}
	
	private static int d2sign(double d) {
		return d > -1.0 && d < 0.0 ? -1 : 1; 
	}
	
	private static String d2s11(double d) {
		String s = Double.toString(d);
		int slen = s.length();
		return s.substring(0, Math.min(11, slen));
	}
	
	private static int degToDeg(double deg) {
		return (int) Math.floor(Math.abs(deg));
	}
	
	private static int degToMin(double deg) {
		double min = 60.0 * (Math.abs(deg) - degToDeg(deg));
		return (int) Math.floor(min);
	}
	
	private static double degToSec(double deg) {
		double sec = 3600.0 * (Math.abs(deg) - degToDeg(deg) - degToMin(deg) / 60.0);
		return sec;
	}
}