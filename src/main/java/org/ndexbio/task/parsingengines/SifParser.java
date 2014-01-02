package org.ndexbio.task.parsingengines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.task.sif.service.SIFNetworkService;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/*
 * Lines in the SIF file specify a source node, a relationship type
 * (or edge type), and one or more target nodes.
 * 
 * see: http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats
 */
public class SifParser implements IParsingEngine
{
    private final File sifFile;
    private final String sifURI;
    private final String extendedBinarySIFEdgeHeader = "PARTICIPANT_A	INTERACTION_TYPE	PARTICIPANT_B	INTERACTION_DATA_SOURCE	INTERACTION_PUBMED_ID";
    private final String extendedBinarySIFAliasHeader = "PARTICIPANT	PARTICIPANT_TYPE	PARTICIPANT_NAME	UNIFICATION_XREF	RELATIONSHIP_XREF";
    private final String extendedBinarySIFPropertiesHeader = "NAME	ORGANISM	URI	DATASOURCE";
    private final List<String> msgBuffer;
    private INetwork network;

    
    
    public SifParser(String fn) throws Exception
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fn), "A filename is required");
        this.msgBuffer = Lists.newArrayList();
        this.sifFile = new File(fn);
        this.sifURI = sifFile.toURI().toString();
    }

    
    
    public List<String> getMsgBuffer()
    {
        return this.msgBuffer;
    }

    public String getSIFURI()
    {
        return sifURI;
    }

    public File getSifFile()
    {
        return sifFile;
    }

    public INetwork getNetwork()
    {
        return network;
    }

    public void setNetwork(INetwork network)
    {
        this.network = network;
    }

    
    
    /**************************************************************************
    * Whitespace (space or tab) is used to delimit the names in the simple
    * interaction file format. However, in some cases spaces are desired in a
    * node name or edge type. The standard is that, if the file contains any
    * tab characters, then tabs are used to delimit the fields and spaces are
    * considered part of the name. If the file contains no tabs, then any
    * spaces are delimiters that separate names (and names cannot contain
    * spaces).
    **************************************************************************/
    public void parseFile()
    {
        try
        {
            this.getMsgBuffer().add("Parsing lines from " + this.getSIFURI());
            BufferedReader bufferedReader;
            
            try
            {
                bufferedReader = new BufferedReader(new FileReader(this.getSifFile()));
            }
            catch (FileNotFoundException e1)
            {
                this.getMsgBuffer().add("Could not find " + this.getSIFURI());
                return;
            }

            boolean extendedBinarySIF = checkForExtendedFormat();
            if (extendedBinarySIF)
            {
                this.processExtendedBinarySIF(bufferedReader);
                SIFNetworkService.getInstance().setFormat("EXTENDED_BINARY_SIF");
            }
            else
            {
                boolean tabDelimited = scanForTabs();
                this.processSimpleSIFLines(tabDelimited, bufferedReader);
                SIFNetworkService.getInstance().setFormat("BINARY_SIF");
            }

            // close database connection
            SIFNetworkService.getInstance().persistNewNetwork();
        }
        catch (Exception e)
        {
            // delete network and close the database connection
            SIFNetworkService.getInstance().rollbackCurrentTransaction();
            e.printStackTrace();
        }
    }

    
    
    private boolean checkForExtendedFormat() throws IOException
    {
        BufferedReader bufferedReader = null;
        try
        {
            bufferedReader = new BufferedReader(new FileReader(this.getSifFile()));
            String line = bufferedReader.readLine();
            // Check the first line for the EBS header
            if (extendedBinarySIFEdgeHeader.equals(line))
            {
                bufferedReader.close();
                return true;
            }
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            bufferedReader.close();
        }

        return false;
    }

    private boolean scanForTabs() throws IOException
    {
        BufferedReader bufferedReader = null;
        try
        {
            bufferedReader = new BufferedReader(new FileReader(this.getSifFile()));
            String line;
            int counter = 0;
            // Check the first 20 lines for tabs
            while ((line = bufferedReader.readLine()) != null)
            {
                if (line.indexOf("\t") != -1)
                    return true;
                if (counter++ > 20)
                    return false;
            }
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            bufferedReader.close();
        }

        return false;
    }

    private void processSimpleSIFLines(boolean tabDelimited, BufferedReader bufferedReader) throws IOException
    {

        try
        {

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                String[] tokens = null;
                if (tabDelimited)
                {
                    tokens = line.split("\t");
                }
                else
                {
                    tokens = line.split("\\s+");
                }

                if (tokens.length == 1)
                    addNode(tokens[0]);
                if (tokens.length == 3)
                    addEdge(tokens[0], tokens[1], tokens[2]);
                // TODO: handle case of multiple object nodes
            }
        }
        catch (IOException e)
        {
            this.getMsgBuffer().add(e.getMessage());
        }
        catch (ExecutionException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            this.getMsgBuffer().add(e.getMessage());
        }
        finally
        {
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
    private void processExtendedBinarySIF(BufferedReader bufferedReader) throws IOException
    {
        try
        {
            // skip the header line
            bufferedReader.readLine();

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                if (line.indexOf(extendedBinarySIFAliasHeader) != -1)
                {
                    processExtendedBinarySIFAliases(bufferedReader);
                    break;
                }
                String[] tokens = null;
                tokens = line.split("\t");
                if (tokens.length > 2)
                {
                    // "PARTICIPANT_A	INTERACTION_TYPE	PARTICIPANT_B	INTERACTION_DATA_SOURCE	INTERACTION_PUBMED_ID";
                    String subject = tokens[0];
                    String predicate = tokens[1];
                    String object = tokens[2];
                    // String dataSource = null; // ignored for now
                    String[] pubMedIds = null;
                    if (tokens.length > 4 && tokens[4] != null)
                    {
                        pubMedIds = tokens[4].split(";");
                    }

                    IEdge edge = addEdge(subject, predicate, object);
                    if (pubMedIds != null)
                    {
                        for (String pubMedId : pubMedIds)
                        {
                            this.addCitation(edge, pubMedId);
                        }
                    }

                }
            }
        }
        catch (IOException e)
        {
            this.getMsgBuffer().add(e.getMessage());
        }
        catch (ExecutionException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            this.getMsgBuffer().add(e.getMessage());
        }
        catch (NdexException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            bufferedReader.close();
        }
    }

    private void addCitation(IEdge edge, String citationString) throws NdexException, ExecutionException
    {
        String[] citationStringElements = citationString.split(":");
        if (citationStringElements.length == 2)
        {
            ICitation citation = SIFNetworkService.getInstance().findOrCreateICitation(citationStringElements[0], citationStringElements[1]);
            edge.addCitation(citation);
        }
    }

    private void processExtendedBinarySIFAliases(BufferedReader bufferedReader) throws IOException
    {

        // "PARTICIPANT	PARTICIPANT_TYPE	PARTICIPANT_NAME	UNIFICATION_XREF	RELATIONSHIP_XREF";
        System.out.println("NO-OP processing for Aliases");
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
            if (line.indexOf(extendedBinarySIFPropertiesHeader) != -1)
            {
                System.out.println("found properties header");
                processExtendedBinarySIFProperties(bufferedReader);
                break;
            }
            else if ("".equals(line))
            {
                // skip blank lines.
            }
            else
            {
                System.out.println("aliases: " + line);
            }
        }

    }

    private void processExtendedBinarySIFProperties(BufferedReader bufferedReader)
    {

        // "NAME	ORGANISM	URI	DATASOURCE";
        // this is currently one line of properties, but perhaps it would be
        // better to have one property per line.
        System.out.println("NO-OP processing for Properties");

    }

    private INode addNode(String name) throws ExecutionException
    {
        IBaseTerm term = findOrCreateBaseTerm(name);
        if (null != term)
        {
            INode node = SIFNetworkService.getInstance().findOrCreateINode(term);
            return node;
        }
        return null;
    }

    private IEdge addEdge(String subject, String predicate, String object) throws ExecutionException
    {
        INode subjectNode = addNode(subject);
        INode objectNode = addNode(object);
        IBaseTerm predicateTerm = findOrCreateBaseTerm(predicate);
        return SIFNetworkService.getInstance().createIEdge(subjectNode, objectNode, predicateTerm);

    }

    private IBaseTerm findOrCreateBaseTerm(String termString) throws ExecutionException
    {
        // case 1 : termString is a URI
        // example: http://identifiers.org/uniprot/P19838
        // treat the last element in the URI as the identifier and the rest as
        // the namespace URI
        // find or create the namespace based on the URI
        // when creating, set the prefix based on the PREFIX-URI table for known
        // namespaces, otherwise do not set.
        //
        IBaseTerm iBaseTerm = null;
        try
        {
            URI termStringURI = new URI(termString);
            String path = termStringURI.getPath();
            if (path.indexOf("/") != -1)
            {
                String identifier = path.substring(path.lastIndexOf('/') + 1);
                String namespaceURI = termString.substring(0, termString.lastIndexOf('/') + 1);
                INamespace namespace = SIFNetworkService.getInstance().findOrCreateINamespace(namespaceURI, null);
                iBaseTerm = SIFNetworkService.getInstance().findOrCreateNodeBaseTerm(identifier, namespace);
                return iBaseTerm;
            }

        }
        catch (URISyntaxException e)
        {
            // ignore and move on to next case
        }

        // case 2: termString is of the form NamespacePrefix:Identifier
        // find or create the namespace based on the prefix
        // when creating, set the URI based on the PREFIX-URI table for known
        // namespaces, otherwise do not set.
        //
        String[] termStringComponents = termString.split(":");
        if (termStringComponents.length == 2)
        {
            String identifier = termStringComponents[1];
            String prefix = termStringComponents[0];
            INamespace namespace = SIFNetworkService.getInstance().findOrCreateINamespace(prefix, null);
            iBaseTerm = SIFNetworkService.getInstance().findOrCreateNodeBaseTerm(identifier, namespace);
            return iBaseTerm;
        }

        // case 3: termString cannot be parsed, use it as the identifier.
        // find or create the namespace for prefix "LOCAL" and use that as the
        // namespace.

        iBaseTerm = SIFNetworkService.getInstance().findOrCreateNodeBaseTerm(termString, SIFNetworkService.getInstance().findOrCreateINamespace(null, "LOCAL"));

        return iBaseTerm;
    }
}
