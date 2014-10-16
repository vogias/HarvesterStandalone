package grnet.metadata.harvester;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uiuc.oai.OAIException;
import uiuc.oai.OAIRecord;
import uiuc.oai.OAIRecordList;
import uiuc.oai.OAIRepository;

public class HarvestAllProcess {
	private static final Logger slf4jLogger = LoggerFactory
			.getLogger(HarvestAllProcess.class);
	private int counter = 0;
	private int deleted = 0;
	private int updated = 0;
	private int newRecords = 0;
	private int unchanged = 0;

	public static void main(String[] args) throws OAIException, IOException,
			JDOMException, ParseException, InterruptedException {

		if (args.length != 4) {
			System.err
					.println("Usage: java HarvestProcess param1(target) param2(foldername) param3(metadataPrefix) param4(set), e.g");
			System.exit(1);
		}
		// else{ throw new IOException("ERRROR");}

		HarvestAllProcess allProcess=new HarvestAllProcess();
		allProcess.listRecords(args[0], args[1], args[2], args[3]);
		
	//	listRecords(args[0], args[1], args[2], args[3]);

		// listRecords("http://jme.collections.natural-europe.eu/oai/","C:/testSet","oai_dc","");
	}

	public synchronized void raiseCounter() {
		counter++;
	}

	public synchronized void raiseDeleted() {
		deleted++;
	}

	public synchronized void raiseNew() {
		newRecords++;
	}

	public synchronized void raiseUpdated() {
		updated++;
	}

	public synchronized void raiseUnchanged() {
		unchanged++;
	}

	public synchronized int getCounter() {
		return counter;
	}

	public synchronized int getUnchanged() {
		return unchanged;
	}

	public synchronized int getNew() {
		return newRecords;
	}

	public synchronized int getUpdated() {
		return updated;
	}

	public synchronized int getDeleted() {
		return deleted;
	}

	public void listRecords(String target, String folderName,
			String metadataPrefix, String set) throws OAIException,
			IOException, JDOMException, ParseException, InterruptedException {
		OAIRepository repos = null;
		//try {
			Properties props = new Properties();
			props.load(new FileInputStream("configure.properties"));

			repos = new OAIRepository();
			repos.setBaseURL(target);

			URL url = new URL(target);
			InetAddress address = InetAddress.getByName(url.getHost());
			String temp = address.toString();
			String IP = temp.substring(temp.indexOf("/") + 1, temp.length());
			System.out.println("OAI-PMH Target IP:" + IP);

			File file = new File(folderName);

			file.mkdirs();

			String granularity = repos.getGranularity();

			System.out.println("Repository granularity:" + granularity);

			SimpleDateFormat dateFormat = null;

			dateFormat = new SimpleDateFormat(
					props.getProperty(Constants.granularity));

			System.out.println("Harvester Dateformat:"
					+ dateFormat.toLocalizedPattern());
			System.out.println("Max retry limit:" + repos.getRetryLimit());

			OAIRecordList records;

			String incremental = props.getProperty(Constants.incremental);

			String fromBase = "1900-01-01";
			String from = "";

			String to = props.getProperty(Constants.to);
			to = dateFormat.format(dateFormat.parse(to));

			String lhDate = props.getProperty(Constants.lhDate);
			lhDate = dateFormat.format(dateFormat.parse(lhDate));

			if (incremental.equalsIgnoreCase("true")) {
				from = lhDate;

				System.out.println("Last harvesting date:" + from);
			} else if (incremental.equalsIgnoreCase("false")) {
				from = dateFormat.format(dateFormat.parse(fromBase));
				// System.out.println("Fallback last harvesting date:" + from);
			} else {
				System.err.println("Wrong harvester.incremental value");
				System.exit(-1);
			}

			String date = dateFormat.format(new Date());

			System.out.println("Harvesting date:" + date);
			System.out.println("Harvesting repository:"
					+ repos.getRepositoryName());

			if (set.equals("")) {

				if (from.equals(dateFormat.format(dateFormat.parse(fromBase))))
					records = repos.listRecords(metadataPrefix);
				else
					records = repos.listRecords(metadataPrefix, to, from);
			}

			else {
				System.out.println("Set:" + set);
				System.out.println("From:" + from);
				records = repos.listRecords(metadataPrefix, to, from, set);
			}
			
			/*String identifier = "";

			int counter = 0;
			int deletedRecords = 0;*/
			

			int threadPoolSize = 1;
			threadPoolSize = Integer.parseInt(props
					.getProperty(Constants.threads));
			int availableProcessors = Runtime.getRuntime()
					.availableProcessors();
			System.out.println("Available cores:" + availableProcessors);
			System.out.println("Thread Pool size:" + threadPoolSize);
			ExecutorService executor = Executors
					.newFixedThreadPool(threadPoolSize);
			long start = System.currentTimeMillis();
			
			
			
			while (records.moreItems()) {

				/*StringBuffer logString = new StringBuffer();

				logString.append(file.getName());
				logString.append(" " + IP);
				logString.append(" " + metadataPrefix);
				logString.append(" " + "ALL");
				
				
*/
				
				OAIRecord item = records.getCurrentItem();
				
				Worker worker=new Worker(file.getName(), IP, metadataPrefix, item, slf4jLogger, this, folderName);
				executor.execute(worker);
				

				/*
				 * get the lom metadata : item.getMetadata(); this return a Node
				 * which contains the lom metadata.
				 */
		/*		if (!item.deleted()) {
					Element metadata = item.getMetadata();

					if (metadata != null) {
						// System.out.println(item.getIdentifier());
						// logString.append(" " + "NEW");
						counter++;
						Record rec = new Record();
						rec.setOaiRecord(item);
						rec.setMetadata(item.getMetadata());
						rec.setOaiIdentifier(item.getIdentifier());

						// logString.append(" " + item.getIdentifier());

						identifier = item.getIdentifier().replaceAll(":", "_");
						identifier = identifier.replaceAll("/", ".");
						identifier = identifier.replaceAll("\\?", ".");

						// File fileTest =new File(folderName + "/" + identifier
						// + ".xml");
						// if(fileTest.exists())
						// System.out.println("File:"+fileTest.getName()+" exists.");

						File mtdt = new File(folderName + "/" + identifier
								+ ".xml");

						if (mtdt.exists()) {
							int fileContent = IOUtilsv2
									.readStringFromFile(mtdt).hashCode();

							int text = OaiUtils.parseLom2Xmlstring(metadata)
									.hashCode();

							if (fileContent != text)
								logString.append(" " + "UPDATED");
							else
								logString.append(" " + "UNCHANGED");
						}

						else
							logString.append(" " + "NEW");

						logString.append(" " + item.getIdentifier());
						IOUtilsv2.writeStringToFileInEncodingUTF8(
								OaiUtils.parseLom2Xmlstring(metadata),
								folderName + "/" + identifier + ".xml");

					} else {

						logString.append(" " + "DELETED");
						logString.append(" " + item.getIdentifier());
						deletedRecords++;

					}
				} else {

					logString.append(" " + "DELETED");
					logString.append(" " + item.getIdentifier());
					deletedRecords++;

				}
				slf4jLogger.info(logString.toString());
				records.moveNext();
			}*/
				records.moveNext();
			// System.out.println(counter);
		/*	System.out.println("Records harvested:" + counter);
			System.out.println("New Records:" + getd);
			System.out.println("Deleted Records :" + counter);
		} catch (OAIException ex) {
			System.err
					.println("Harvesting from "
							+ repos.getRepositoryName()
							+ " did not complete because of a harvesting error, the error was : "
							+ ex.getMessage());
		}*/

	}
			executor.shutdown();

			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			long end = System.currentTimeMillis();
			long diff = end - start;

			
			System.out.println("=============Harvesting Statistics=============");
			System.out.println("Records harvested:" + getCounter());
			System.out.println("New Records:" + getNew());
			System.out.println("Deleted Records:" + getDeleted());
			System.out.println("Updated Records:" + getUpdated());
			System.out.println("Unchanged Records:" + getUnchanged());
			System.out.println("Duration:" +diff+" ms");
  }
}
