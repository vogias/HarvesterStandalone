package gr.agroknow.metadata.harvester;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
			JDOMException {

		if (args.length != 3) {
			System.err
					.println("Usage: java HarvestProcess param1(target) param2(foldername) param3(metadataPrefix), e.g");
			System.exit(1);
		}
		// else{ throw new IOException("ERRROR");}

		listRecords(args[0], args[1], args[2]);

		// listRecords("http://jme.collections.natural-europe.eu/oai/","C:/testSet","oai_dc","");
	}

	public static void listRecords(String target, String folderName,
			String metadataPrefix) throws OAIException, IOException,
			JDOMException {

		OAIRepository repos = new OAIRepository();
		File file = new File(folderName);
		String identifier = "";
		file.mkdirs();

		repos.setBaseURL(target);

		OAIRecordList records;

		// OAIRecordList records =
		// repos.listRecords("ese","9999-12-31","2000-12-31","");

		System.out.println("Harvesting date:" + new Date().toString());

		System.out
				.println("Harvesting repository:" + repos.getRepositoryName());

		records = repos.listRecords(metadataPrefix);

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
					IOUtilsv2.writeStringToFileInEncodingUTF8(
							OaiUtils.parseLom2Xmlstring(metadata), folderName
									+ "/" + identifier + ".xml");

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

	}

}
