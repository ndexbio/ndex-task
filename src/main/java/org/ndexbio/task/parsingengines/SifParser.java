package org.ndexbio.task.parsingengines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.persistence.orientdb.NDExNoTxMemoryPersistence;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.task.service.network.SIFNetworkService;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;


/*
 * Lines in the SIF file specify a source node, a relationship type
 * (or edge type), and one or more target nodes.
 * 
 * see: http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats
 */

//TODO: need to load format and source later. -- cj
public class SifParser implements IParsingEngine {
	private final File sifFile;
	private final String sifURI;
	private final String extendedBinarySIFEdgeHeader = "PARTICIPANT_A	INTERACTION_TYPE	PARTICIPANT_B	INTERACTION_DATA_SOURCE	INTERACTION_PUBMED_ID";
	private final String extendedBinarySIFAliasHeader = "PARTICIPANT	PARTICIPANT_TYPE	PARTICIPANT_NAME	UNIFICATION_XREF	RELATIONSHIP_XREF";
	private final String extendedBinarySIFPropertiesHeader = "NAME	ORGANISM	URI	DATASOURCE";
	private final List<String> msgBuffer;
	private INetwork network;
	private NDExNoTxMemoryPersistence persistenceService;

	public SifParser(String fn, String ownerName) throws Exception {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A filename is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName),
				"A network owner name is required");
		this.msgBuffer = Lists.newArrayList();
		this.sifFile = new File(fn);
		this.sifURI = sifFile.toURI().toString();
		this.persistenceService = new NDExNoTxMemoryPersistence(new NdexDatabase());
		
		String title =  Files.getNameWithoutExtension(this.sifFile.getName());

		persistenceService.createNewNetwork(ownerName, title, null);

	}

	public List<String> getMsgBuffer() {
		return this.msgBuffer;
	}

	public String getSIFURI() {
		return sifURI;
	}

	public File getSifFile() {
		return sifFile;
	}

	public INetwork getNetwork() {
		return network;
	}

/*	
	private void setNetwork() throws Exception {
		String title = Files.getNameWithoutExtension(this.sifFile.getName());
		this.networkService.createNewNetwork(this.getOwnerName(), title);
	}
*/
	/**************************************************************************
	 * Whitespace (space or tab) is used to delimit the names in the simple
	 * interaction file format. However, in some cases spaces are desired in a
	 * node name or edge type. The standard is that, if the file contains any
	 * tab characters, then tabs are used to delimit the fields and spaces are
	 * considered part of the name. If the file contains no tabs, then any
	 * spaces are delimiters that separate names (and names cannot contain
	 * spaces).
	 **************************************************************************/
	public void parseFile() {
		try {

			this.getMsgBuffer().add("Parsing lines from " + this.getSIFURI());
			BufferedReader bufferedReader;

			try {
				bufferedReader = new BufferedReader(new FileReader(
						this.getSifFile()));
			} catch (FileNotFoundException e1) {
				this.getMsgBuffer().add("Could not find " + this.getSIFURI());
				return;
			}

			boolean extendedBinarySIF = checkForExtendedFormat();
			if (extendedBinarySIF) {
//				this.networkService.setFormat("EXTENDED_BINARY_SIF");
			} else {
				boolean tabDelimited = scanForTabs();
				this.processSimpleSIFLines(tabDelimited, bufferedReader);
//				this.networkService.setFormat("BINARY_SIF");
			}

			// close database connection
			this.persistenceService.persistNetwork();
		} catch (Exception e) {
			// delete network and close the database connection
			this.persistenceService.abortTransaction();
			e.printStackTrace();
		}
	}

	private boolean checkForExtendedFormat() throws IOException {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(
					this.getSifFile()));
			String line = bufferedReader.readLine();
			// Check the first line for the EBS header
			if (extendedBinarySIFEdgeHeader.equals(line)) {
				bufferedReader.close();
				return true;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			bufferedReader.close();
		}

		return false;
	}

	private boolean scanForTabs() throws IOException {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(
					this.getSifFile()));
			String line;
			int counter = 0;
			// Check the first 20 lines for tabs
			while ((line = bufferedReader.readLine()) != null) {
				if (line.indexOf("\t") != -1)
					return true;
				if (counter++ > 20)
					return false;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			bufferedReader.close();
		}

		return false;
	}

	private void processSimpleSIFLines(boolean tabDelimited,
			BufferedReader bufferedReader) throws IOException, ExecutionException {

		try {

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = null;
				if (tabDelimited) {
					tokens = line.split("\t");
				} else {
					tokens = line.split("\\s+");
				}

				if (tokens.length == 1)
					addNode(tokens[0]); 
				if (tokens.length == 3)
					addEdge(tokens[0], tokens[1], tokens[2]);  
				// TODO: handle case of multiple object nodes
			}
		} catch (IOException e) {
			this.getMsgBuffer().add(e.getMessage());
		} /*catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.getMsgBuffer().add(e.getMessage());
		} */finally {
			bufferedReader.close();
		}
	}

	/*
	 * Standard Extended Binary SIF has two sections, one for edges and one to
	 * define aliases for terms used in the edges. NDEx Extended Binary SIF has
	 * an additional section that captures some additional network meta-data
	 * 
	 * Each section is preceded by its header, so the parsing mode switches as
	 * each header is encountered. We already know that line 0 is the edge
	 * header so we start processing edges on the next line.
	 */
	private void processExtendedBinarySIF(BufferedReader bufferedReader)
			throws IOException {
		try {
			// skip the header line
			bufferedReader.readLine();

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.indexOf(extendedBinarySIFAliasHeader) != -1) {
			//		processExtendedBinarySIFAliases(bufferedReader);  cj
					break;
				}
				String[] tokens = null;
				tokens = line.split("\t");
				if (tokens.length > 2) {
					// "PARTICIPANT_A	INTERACTION_TYPE	PARTICIPANT_B	INTERACTION_DATA_SOURCE	INTERACTION_PUBMED_ID";
					String subject = tokens[0];
					String predicate = tokens[1];
					String object = tokens[2];
					// String dataSource = null; // ignored for now
					String[] pubMedIds = null;
					if (tokens.length > 4 && tokens[4] != null) {
						pubMedIds = tokens[4].split(";");
					}

					Edge edge = addEdge(subject, predicate, object);  --cj
					if (pubMedIds != null) {
						for (String pubMedId : pubMedIds) {
					//		this.addCitation(edge, pubMedId);
						}
					}

				}
			}
		} catch (IOException e) {
			this.getMsgBuffer().add(e.getMessage());
		} /*catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.getMsgBuffer().add(e.getMessage());
		} */finally {
			bufferedReader.close();
		}
	}

	//commented out for now. -- cj
/*	private void addCitation(IEdge edge, String citationString)
			throws NdexException, ExecutionException {
		String[] citationStringElements = citationString.split(":");
		if (citationStringElements.length == 2) {
			ICitation citation = this.networkService.findOrCreateICitation(
					citationStringElements[0], citationStringElements[1]);
			edge.addCitation(citation);
		}
	} */
/*
	private void processExtendedBinarySIFAliases(BufferedReader bufferedReader)
			throws IOException, ExecutionException {

		// "PARTICIPANT	PARTICIPANT_TYPE	PARTICIPANT_NAME	UNIFICATION_XREF	RELATIONSHIP_XREF";
		System.out.println("Processing Aliases");
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			System.out.println("-- " + line);
			if (line.indexOf(extendedBinarySIFPropertiesHeader) != -1) {
				System.out.println("found properties header");
				processExtendedBinarySIFProperties(bufferedReader);
				break;
			} else if ("".equals(line)) {
				// skip blank lines.
			} else {
				// System.out.println("aliases: " + line);
				// Process one line of aliases
				String[] tokens = null;
				tokens = line.split("\t");
				if (tokens.length > 2) {
					String participantIdentifier = tokens[0];
					// find the node that represents the term specified by the
					// participantIdentifier
					INode participant = findNode(participantIdentifier);
					if (participant == null)
						break;
					//String type = tokens[1];
					String name = tokens[2];
					// special case processing for "_HUMAN" suffix
					int humanSuffixIndex = name.indexOf("_HUMAN");
					if (humanSuffixIndex != -1){
						name = name.substring(0, humanSuffixIndex);
					}
					participant.setName(name);
					participant.addAlias(participant.getRepresents());
					if (tokens.length > 3) {
						String[] unificationAliases = tokens[3].split(";");
						for (String alias : unificationAliases) {
							participant.addAlias(findOrCreateBaseTerm(alias));
						}
						if (tokens.length > 4) {
							String[] relationshipAliases = tokens[4].split(";");
							for (String alias : relationshipAliases) {
								participant
										.addRelatedTerm(findOrCreateBaseTerm(alias));
							}
						}
					}
				}
			}
		}
	}
*/
	private void processExtendedBinarySIFProperties(
			BufferedReader bufferedReader) throws IOException {

		// NAME\tORGANISM\tURI\tDATASOURCE";
		// this is currently one line of properties, but perhaps it would be
		// better to have one property per line.
		System.out.println("Processing one line of Network Properties");
		String line = bufferedReader.readLine();
		if (line != null) {
			String[] values = line.split("\t");
			if (values.length > 1 && values[0] != null) {
				System.out.println("Description: " + values[0]);
				this.persistenceService.getCurrentNetwork().setDescription(values[0]);
				this.persistenceService.getCurrentNetwork().setName(values[1]);
				this.persistenceService.getNetworkDoc().field(NdexClasses.Network_P_desc, values[0])
				  .field(NdexClasses.Network_P_name, values[0])
				  .save();
			}

			if (values.length > 0 && values[0] != null) {
				System.out.println("Description: " + values[0]);
				this.persistenceService.getCurrentNetwork().setDescription(values[0]);
				this.persistenceService.getNetworkDoc().field(NdexClasses.Network_P_desc, values[0])
				  .save();
			}
			
			if (values.length > 3 && values[3] != null) {
				System.out.println("Source: " + values[3]);
				String source = values[3];
				if (source.equals("http://purl.org/pc2/4/pid")){
					source = "PID";
				}
//				this.networkService.setSource(source);
			}
			
		}
	}

	private Node addNode(String name) throws ExecutionException {
	/*	BaseTerm term = persistenceService.getBaseTerm(name);
		
		if ( term == null) 
			throw new NdexException ("Internal Error: Failed to get Baseterm " + name + "from persistenceService.");
	*/	
		Node node = persistenceService.getNodeByBaseTerm(name);
		return node;
	}


	private IEdge addEdge(String subject, String predicate, String object)
			throws ExecutionException {
		Node subjectNode = addNode(subject);
		Node objectNode = addNode(object);
		BaseTerm predicateTerm = persistenceService.getBaseTerm(predicate);
		return this.networkService.createIEdge(subjectNode, objectNode,
				predicateTerm);

	}
	
}
