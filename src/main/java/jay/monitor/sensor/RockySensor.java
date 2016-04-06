package jay.monitor.sensor;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.github.baloise.rocky.Rocky;

import jay.monitor.sensor.impl.AbstractSensor;

public class RockySensor extends AbstractSensor implements ActionListener {
	String view;
	Rocky rocky;
	private URL url;

	@Override
	public void configure(Properties properties) {
		super.configure(properties);
		view = properties.getProperty("view");
		String jenkinsUrl = properties.getProperty("jenkinsUrl");
		String rockyUrl = properties.getProperty("rockyUrl");
		String rockyUser = properties.getProperty("rockyUser");
		String rockyPass = properties.getProperty("rockyPass");
		
		String viewPattern = "\"view/"+view+".*?->\\s*(\\w+)";
		//	String jobPattern = "\""+view+", #\\d+, ([A-Z]+)";
		Pattern p = Pattern.compile(viewPattern);
		rocky = new Rocky(rockyUser, rockyPass, rockyUrl)
				.withStopMessage("RockySensor "+view+" stop").withHandler(s -> {
					Matcher m = p.matcher(s);
					if (m.find()) {
						String result = m.group(1);
						if ("FAILURE".equals(result)) {
							setValue(0);
						} else if ("UNSTABLE".equals(result)) {
							setValue(0.5);
						} else if ("SUCCESS".equals(result)) {
							setValue(1);
						}
					}
					
				});
		
		
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				new Thread() {

					public void run() {
						try {
							url = new URL(jenkinsUrl+"/view/"+view+"/api/json");
							String tmp = readUrl(url);
							if(tmp.contains("\"color\":\"red")) {
								setValue(0);
							} else if(tmp.contains("\"color\":\"yellow")) {
								setValue(0.5);
							} else if(tmp.contains("\"color\":\"blue")) {
								setValue(1);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				}.start();
			}
		});
	}
	
	@Override
	public void run() {
		rocky.start();
		try {
			rocky.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public String getName() {
		return view;
	}

	public String readUrl(URL url) throws IOException {
	    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
	    String inputLine;
	    StringBuffer ret = new StringBuffer();
	    while ((inputLine = in.readLine()) != null) {
	      ret.append(inputLine);
	      ret.append("\n");
	    }
	    in.close();
	    return ret.toString();
	  }

	 @Override
	  public void actionPerformed(ActionEvent e) {
	    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
	    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
	        try {
	            desktop.browse(url.toURI());
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }
	    }
	  }
	
}
