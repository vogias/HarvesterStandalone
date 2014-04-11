package gr.agroknow.metadata.harvester;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.ariadne.util.IOUtilsv2;
import org.ariadne.util.OaiUtils;
import org.jdom.Element;
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

	public static void main(String[] args) throws OAIException, IOException,
			JDOMException, ParseException {

		if (args.length != 4) {
			System.err
					.println("Usage: java HarvestProcess param1(target) param2(foldername) param3(metadataPrefix) param4(set), e.g");
			System.exit(1);
		}
		// else{ throw new IOException("ERRROR");}

		listRecords(args[0], args[1], args[2], args[3]);

		// listRecords("http://jme.collections.natural-europe.eu/oai/","C:/testSet","oai_dc","");
	}

	public static void listRecords(String target, String folderName,
			String metadataPrefix, String set) throws OAIException,
			IOException, JDOMException, ParseException {
		OAIRepository repos = null;
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("configure.properties"));

			repos = new OAIRepository();
			repos.setBaseURL(target);

			File file = new File(folderName);
			String identifier = "";
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

			String to = "9999-12-30";
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

			// records = repos.listRecords(metadataPrefix);

			int counter = 0;
			int deletedRecords = 0;
			// records.moveNext();
			while (records.moreItems()) {

				StringBuffer logString = new StringBuffer();

				logString.append(file.getName());
				logString.append(" " + metadataPrefix);
				logString.append(" " + "ALL");

				OAIRecord item = records.getCurrentItem();

				/*
				 * get the lom metadata : item.getMetadata(); this return a Node
				 * which contains the lom metadata.
				 */
				if (!item.deleted()) {
					Element metadata = item.getMetadata();

					logString.append(" " + "NEW");

					if (metadata != null) {
						// System.out.println(item.getIdentifier());
						counter++;
						Record rec = new Record();
						rec.setOaiRecord(item);
						rec.setMetadata(item.getMetadata());
						rec.setOaiIdentifier(item.getIdentifier());

						logString.append(" " + item.getIdentifier());

						identifier = item.getIdentifier().replaceAll(":", "_");
						identifier = identifier.replaceAll("/", ".");
						identifier = identifier.replaceAll("\\?", ".");
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
			}
			// System.out.println(counter);
			System.out.println("Records harvested:" + counter);
		} catch (OAIException ex) {
			System.err
					.println("Harvesting from "
							+ repos.getRepositoryName()
							+ " did not complete because of a harvesting error, the error was : "
							+ ex.getMessage());
		}

	}
}
