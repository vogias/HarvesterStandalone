/**
 * 
 */
package grnet.metadata.harvester;

import java.io.File;
import java.io.IOException;

import org.ariadne.util.IOUtilsv2;
import org.ariadne.util.OaiUtils;
import org.jdom.Element;
import org.slf4j.Logger;

import uiuc.oai.OAIException;
import uiuc.oai.OAIRecord;

/**
 * @author vogias
 * 
 */
public class Worker implements Runnable {

	String name, IP, metadataPrefix, folderName;
	OAIRecord item;
	Logger slf4jLogger;
	HarvestAllProcess allProcess;

	public Worker(String name, String IP, String metadataPrefix,
			OAIRecord item, Logger slf4jLogger, HarvestAllProcess allProcess,
			String folderName) {

		this.name = name;
		this.IP = IP;
		this.metadataPrefix = metadataPrefix;
		this.item = item;
		this.slf4jLogger = slf4jLogger;
		this.folderName = folderName;
		this.allProcess = allProcess;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		String identifier = "";

		StringBuffer logString = new StringBuffer();

		logString.append(name);
		logString.append(" " + IP);
		logString.append(" " + metadataPrefix);
		logString.append(" " + "ALL");

		/*
		 * get the lom metadata : item.getMetadata(); this return a Node which
		 * contains the lom metadata.
		 */
		try {
			if (!item.deleted()) {
				Element metadata;

				metadata = item.getMetadata();

				if (metadata != null) {
					// System.out.println(item.getIdentifier());
					// logString.append(" " + "NEW");
					allProcess.raiseCounter();
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

					File mtdt = new File(folderName + "/" + identifier + ".xml");

					if (mtdt.exists()) {
						int fileContent = IOUtilsv2.readStringFromFile(mtdt)
								.hashCode();

						int text = OaiUtils.parseLom2Xmlstring(metadata)
								.hashCode();

						if (fileContent != text) {
							logString.append(" " + "UPDATED");
							allProcess.raiseUpdated();
						} else {
							logString.append(" " + "UNCHANGED");
							allProcess.raiseUnchanged();
						}
					}

					else {
						logString.append(" " + "NEW");
						allProcess.raiseNew();
					}

					logString.append(" " + item.getIdentifier());
					try {
						IOUtilsv2.writeStringToFileInEncodingUTF8(
								OaiUtils.parseLom2Xmlstring(metadata),
								folderName + "/" + identifier + ".xml");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {

					logString.append(" " + "DELETED");
					logString.append(" " + item.getIdentifier());
					allProcess.raiseDeleted();

				}
			} else {

				logString.append(" " + "DELETED");
				logString.append(" " + item.getIdentifier());
				allProcess.raiseDeleted();

			}
			slf4jLogger.info(logString.toString());
			// records.moveNext();
		} catch (OAIException e1) {
			System.err
					.println("Harvesting from "
							+ name
							+ " did not complete because of a harvesting error, the error was : "
							+ e1.getMessage());
		}

	}

}
