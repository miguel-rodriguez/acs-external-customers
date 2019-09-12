package com.alfresco.support.alfrescoCMIS.model;

public class CMISObject {
	private String name;
	private String id;
	private String nodeRef;
	private String baseTypeId;
	private String path;
	private String createdBy;
	private String lastModifiedBy;
	private String lastModificationDate;
	private String version;
	private Integer size;
	private boolean canDelete;
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName(){
		return this.name;
	}
	
	public void setNodeRef(String nodeRef) {
		this.nodeRef = nodeRef;
	}

	public String getNodeRef(){
		return this.nodeRef;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId(){
		return this.id;
	}
	
	public void setBaseTypeId(String baseTypeId) {
		this.baseTypeId = baseTypeId;
	}

	public String getBaseTypeId(){
		return this.baseTypeId;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	public String getPath(){
		return this.path;
	}
	
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedBy(){
		return this.createdBy;
	}
	
	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	public String getLastModifiedBy(){
		return this.lastModifiedBy;
	}
	
	public void setLastModificationDate(String lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public String getLastModificationDate(){
		return this.lastModificationDate;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public Integer getSize(){
		return this.size;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion(){
		return this.version;
	}
	
	public void setCanDelete(boolean canDelete) {
		this.canDelete = canDelete;
	}
	
	public boolean getCanDelete(){
		return this.canDelete;
	}

	@Override
	public String toString() {
		return "CMISObject [name=" + name + ", id=" + id + ", nodeRef=" + nodeRef + ", baseTypeId=" + baseTypeId
				+ ", path=" + path + ", createdBy=" + createdBy + ", lastModifiedBy=" + lastModifiedBy
				+ ", lastModificationDate=" + lastModificationDate + ", version=" + version + ", size=" + size
				+ ", canDelete=" + canDelete + "]";
	}
}