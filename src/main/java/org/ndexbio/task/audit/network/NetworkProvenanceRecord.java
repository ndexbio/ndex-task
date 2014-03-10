package org.ndexbio.task.audit.network;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.google.common.base.Objects;

/*
 * Represents a set of attributes used to track the creation and
 * evolution of a NDEx network
 */
public class NetworkProvenanceRecord {
	private final NetworkIdentifier networkId;
	
	private final Date createdDate;
	private final Date lastModifiedDate;
	private final Boolean original;
	private final UUID sourceUUID;
	private final Long validityMetric;
	
	public NetworkProvenanceRecord( NetworkIdentifier anId,
			 Date cDate, Date mDate, Boolean orig, UUID aSource,
			  Long aCount ){
		this.networkId = anId;
		
		this.createdDate = Objects.firstNonNull(cDate, new Date());
		this.lastModifiedDate = Objects.firstNonNull(mDate, new Date());
		this.original = Objects.firstNonNull(orig, Boolean.FALSE);
		this.sourceUUID = aSource;
		this.validityMetric = aCount;		
	}

	public NetworkIdentifier getNetworkId() {
		return networkId;
	}


	public Date getCreatedDate() {
		return createdDate;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public Boolean getOriginal() {
		return original;
	}

	public UUID getSourceUUID() {
		return sourceUUID;
	}

	public Long getEdgeCount() {
		return validityMetric;
	}
	public String toString() {
	     return ReflectionToStringBuilder.toString(this);
	 }
	
	public boolean equals(Object obj) {
		   return EqualsBuilder.reflectionEquals(this, obj);
		 }
	public int hashCode() {
		   return HashCodeBuilder.reflectionHashCode(this);
		 }

}
