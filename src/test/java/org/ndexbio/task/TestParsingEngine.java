package org.ndexbio.task;

import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.data.Permissions;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;
import org.ndexbio.task.parsingengines.*;
import org.ndexbio.task.service.network.SIFNetworkService;

public class TestParsingEngine
{
    private static String _testUserName = "dexterpratt";
    private static IUser _testUser = null;

    @BeforeClass
    public static void setupUser() throws Exception
    {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setSearchString(_testUserName);
        searchParameters.setSkip(0);
        searchParameters.setTop(1);

        try
        {
        	
            SearchResult<IUser> result = (new SIFNetworkService()).findUsers(searchParameters);
            _testUser = (IUser)result.getResults().iterator().next(); 
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    
/*
    @Test
    public void parseLargeExcelFile() throws Exception
    {
        final URL smallExcelNetworkUrl = getClass().getResource("/resources/large-excel-network.xls");
        final ExcelParser excelParser = new ExcelParser(smallExcelNetworkUrl.toURI().getPath());
//        excelParser.setNetwork(SIFNetworkService.getInstance().createNewNetwork());
//        INetworkMembership membership = SIFNetworkService.getInstance().createNewMember();
//        membership.setMember(_testUser);
//        membership.setPermissions(Permissions.ADMIN);
//        INetwork network = excelParser.getNetwork();
//        membership.setNetwork(network);
//        network.setIsPublic(true);
//        
//        _testUser.addNetwork(membership);
//        network.addMember(membership);

        excelParser.parseFile();
    }

    @Test
    public void parseSifFile() throws Exception
    {
        final URL galNetworkUrl = getClass().getResource("/resources/gal-filtered.sif");
        final SifParser sifParser = new SifParser(galNetworkUrl.toURI().getPath());
        sifParser.setNetwork(SIFNetworkService.getInstance().createNewNetwork());
        INetworkMembership membership = SIFNetworkService.getInstance().createNewMember();
        membership.setMember(_testUser);
        membership.setPermissions(Permissions.ADMIN);
        INetwork network = sifParser.getNetwork();
        membership.setNetwork(network);
        network.setIsPublic(true);
        
        _testUser.addNetwork(membership);
        network.addMember(membership);

        sifParser.parseFile();
    }

*/
    
    @Test
    public void parseLargeSifFile() throws Exception
    {
        final URL galNetworkUrl = getClass().getResource("/resources/Glucocorticoid_receptor_regulatory_network.SIF");
        final SifParser sifParser = new SifParser(galNetworkUrl.toURI().getPath(),_testUserName);
        System.out.println("Parsing SIF : " + galNetworkUrl.toURI().getPath());
        sifParser.parseFile();
    }

    @Test
    public void parseXbelFile() throws Exception
    {
        final URL xbelNetworkURL = getClass().getResource("/resources/tiny-corpus.xbel");
        final XbelParser xbelParser = new XbelParser(xbelNetworkURL.toURI().getPath(),
        		_testUserName);

        if (!xbelParser.getValidationState().isValid())
            Assert.fail("tiny-corpus.xbel is invalid.");
        System.out.println("Parsing XBEL : " + xbelNetworkURL.toURI().getPath());
        xbelParser.parseFile();
    }
    @Test
    public void parseExcelFile() throws Exception
    {
        final URL smallExcelNetworkUrl = getClass().getResource("/resources/small-excel-network.xls");
        final ExcelParser excelParser = new ExcelParser(smallExcelNetworkUrl.toURI().getPath(),
        		_testUserName);

        System.out.println("Parsing Excel file : " + smallExcelNetworkUrl.toURI().getPath());
       excelParser.parseFile();
    }
/*
    @Test
    public void parseLargeXbelFile() throws Exception
    {
        final URL galNetworkUrl = getClass().getResource("/resources/small-corpus.xbel");
        final XbelParser xbelParser = new XbelParser(galNetworkUrl.toURI().getPath(),	_testUserName);

        if (!xbelParser.getValidationState().isValid())
            Assert.fail("small-corpus.xbel is invalid.");
        


        xbelParser.parseFile();
    }
    */
}