package gr.agroknow.metadata.harvester;

import java.io.File;
import java.io.IOException;
import java.util.Date;

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

		StringBuffer logString = new StringBuffer();

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

		logString.append(file.getName());
		System.out.println("Folder:" + folderName);
		logString.append(" " + folderName);
		System.out.println("Target URL:" + target);
		logString.append(" " + target);
		System.out.println("Metadata prefix:" + metadataPrefix);
		logString.append(" " + metadataPrefix);
		System.out.println("Repository set:ALL");
		logString.append(" " + "ALL");

		records = repos.listRecords(metadataPrefix);
		

		int counter = 0;
		// records.moveNext();
		while (records.moreItems()) {
			counter++;
			OAIRecord item = records.getCurrentItem();

			/*
			 * get the lom metadata : item.getMetadata(); this return a Node
			 * which contains the lom metadata.
			 */
			if (!item.deleted()) {
				Element metadata = item.getMetadata();
				if (metadata != null) {
					// System.out.println(item.getIdentifier());
					Record rec = new Record();
					rec.setOaiRecord(item);
					rec.setMetadata(item.getMetadata());
					rec.setOaiIdentifier(item.getIdentifier());
					identifier = item.getIdentifier().replaceAll(":", "_");
					identifier = identifier.replaceAll("/", ".");
					IOUtilsv2.writeStringToFileInEncodingUTF8(
							OaiUtils.parseLom2Xmlstring(metadata), folderName
									+ "/" + identifier + ".xml");

				} else {
					System.out.println(item.getIdentifier() + " deleted");
				}
			} else {
				System.out.println(item.getIdentifier() + " deleted");
			}
			records.moveNext();
		}
		// System.out.println(counter);
		System.out.println("Records harvested:" + counter);
		logString.append(" " + counter);
		slf4jLogger.info(logString.toString());
	}

}
