package org.ndexbio.task;

import java.net.URL;
import org.junit.Test;
import org.ndexbio.task.parsingengines.ExcelParser;
import org.ndexbio.task.parsingengines.SifParser;
import org.ndexbio.xbel.parser.XbelFileParser;

public class TestParsingEngine
{
    @Test
    public void parseExcelFile() throws Exception
    {
        final URL smallExcelNetworkUrl = getClass().getResource("/resources/small-excel-network.xls");
        final ExcelParser excelParser = new ExcelParser(smallExcelNetworkUrl.toURI().getPath());
        excelParser.parseExcelFile();
    }
    
    @Test
    public void parseLargeExcelFile() throws Exception
    {
        final URL smallExcelNetworkUrl = getClass().getResource("/resources/large-excel-network.xls");
        final ExcelParser excelParser = new ExcelParser(smallExcelNetworkUrl.toURI().getPath());
        excelParser.parseExcelFile();
    }
    
    @Test
    public void parseSifFile() throws Exception
    {
        final URL galNetworkUrl = getClass().getResource("/resources/gal-filtered.sif");
        final SifParser sifParser = new SifParser(galNetworkUrl.toURI().getPath());
        sifParser.parseSIFFile();
    }
    
    @Test
    public void parseXbelFile() throws Exception
    {
        final URL galNetworkUrl = getClass().getResource("/resources/tiny-corpus.xbel");
        final XbelFileParser xbelParser = new XbelFileParser(galNetworkUrl.toURI().getPath());
        
        if (xbelParser.getValidationState().isValid())
            xbelParser.parseXbelFile();
    }
    
    @Test
    public void parseLargeXbelFile() throws Exception
    {
        final URL galNetworkUrl = getClass().getResource("/resources/small-corpus.xbel");
        final XbelFileParser xbelParser = new XbelFileParser(galNetworkUrl.toURI().getPath());
        
        if (xbelParser.getValidationState().isValid())
            xbelParser.parseXbelFile();
    }
}