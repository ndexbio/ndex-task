package org.ndexbio.task.parsingengines;

import org.ndexbio.common.exceptions.NdexException;

public interface IParsingEngine
{
    /**************************************************************************
    * Parses the specified file. 
    **************************************************************************/
    public void parseFile() throws  NdexException;
}
