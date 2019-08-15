/**
* This code uses Apache Chemistry (http://chemistry.apache.org/).
* License accords to Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
*/

package com.alfresco.support.alfrescoCMIS.model;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.DocumentProperties;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.client.util.FileUtils;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CMISSession {
	private String currentPath;
    private final static Logger logger = LoggerFactory.getLogger(CMISSession.class);
    private Session session;
    
	/**
	 * Connect to alfresco repository
	 * 
	 * @return session object
	 */
	public Session connect(String ALFRESCO_ATOMPUB_URL, String REPOSITORY_ID, String username, String password) {
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put(SessionParameter.USER, username);
		parameter.put(SessionParameter.PASSWORD, password);
		parameter.put(SessionParameter.ATOMPUB_URL, ALFRESCO_ATOMPUB_URL);
		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		parameter.put(SessionParameter.REPOSITORY_ID, REPOSITORY_ID);

		this.session = sessionFactory.createSession(parameter);
		return session;
	}
	
	public Session getSession() {
		return this.session;
	}
	
	/**
	 * Search alfresco repository
	 * 
	 * @return list of object
	 */
	public List <CMISObject> search(Session session, String root_folder, String term) {
		
    	CmisObject rootFolderObject = session.getObjectByPath(root_folder);
    	String rootFolderNodeRef = rootFolderObject.getPropertyValue("alfcmis:nodeRef").toString();

		List <CMISObject> objects = new ArrayList<CMISObject>();
		CMISObject node = new CMISObject();
		
		logger.debug("Query: SELECT * FROM cmis:document WHERE IN_TREE('" +  rootFolderNodeRef + "') AND (CONTAINS('" + term + "') OR cmis:name like '%" + term + "%')");

		//ItemIterable<QueryResult> results = session.query("SELECT * FROM cmis:document WHERE IN_TREE('" +  rootFolderNodeRef + "') and cmis:name like '%" + term + "%'", false);
		ItemIterable<QueryResult> results = session.query("SELECT * FROM cmis:document WHERE IN_TREE('" +  rootFolderNodeRef + "') AND (CONTAINS('" + term + "') OR cmis:name like '%" + term + "%')", false);
		
		logger.debug("Search results: " + results.getTotalNumItems());
		List<String> uniqueResults = new ArrayList<String>();
		for(QueryResult hit: results) { 
			if (hit == null) {
				break;
			}
			
			String nodeRef=hit.getPropertyById("alfcmis:nodeRef").getFirstValue().toString();
			if(session.getObject(nodeRef) == null) {
				logger.debug("Node does not exist: " + nodeRef );
				continue;
			}
			// avoid duplicate search results
			if (uniqueResults.contains(nodeRef.trim())){
				logger.debug("Duplicate result detected: " + nodeRef );
				continue;
			}
			uniqueResults.add(nodeRef);
			node.setNodeRef(nodeRef);
			node.setName(hit.getPropertyById("cmis:name").getFirstValue().toString());
			node.setBaseTypeId(hit.getPropertyById("cmis:baseTypeId").getFirstValue().toString());
			node.setCreatedBy(hit.getPropertyById("cmis:createdBy").getFirstValue().toString());
			node.setLastModifiedBy(hit.getPropertyById("cmis:lastModifiedBy").getFirstValue().toString());
			if (hit.getPropertyById("cmis:versionLabel").getFirstValue().toString() != null) {
				node.setVersion(hit.getPropertyById("cmis:versionLabel").getFirstValue().toString());
			}
		
			if (hit.getPropertyById("cmis:contentStreamLength").getFirstValue().toString() != null) {
				Integer size = Integer.valueOf(hit.getPropertyById("cmis:contentStreamLength").getFirstValue().toString()) / 1000;
				node.setSize(size);
			}
			
			// Convert modification date to string
	        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");	
	        Calendar calendar = (Calendar) hit.getPropertyById("cmis:lastModificationDate").getFirstValue();
			node.setLastModificationDate(sdf.format(calendar.getTime()));
						
			CmisObject cmisObject = session.getObject(nodeRef);
			// we know it is a filable object, we just retrieved it by path
			FileableCmisObject fileableCmisObject = (FileableCmisObject) cmisObject;
			// get all paths, there must be at least one
			String path = fileableCmisObject.getPaths().get(0);
			int index = path.lastIndexOf("/");
			node.setPath(path.substring(0, index));		

			Action canDelete = Action.CAN_DELETE_OBJECT;
			
			if (cmisObject.getAllowableActions().getAllowableActions().contains(canDelete)) {
			       node.setCanDelete(true); 
			}
			
			objects.add(node);
			node = new CMISObject();
		
		    for(PropertyData<?> property: hit.getProperties()) {
				String queryName = property.getQueryName();
				Object value = property.getFirstValue();
				if (queryName.equals("cmis:name")){
					node.setName(value.toString());
				}
			}
		}
		return objects;
	}
	
	public  String getCurrentPath(String downloadFiles) {
		return currentPath;
	}
	
	public ZipOutputStream downloadMultipleFiles(String downloadFiles){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Boolean> nodes;
        String zipFile = System.getProperty("java.io.tmpdir") + "/content.zip";

		List<String> srcFiles = new ArrayList<String>();

		try {
			nodes = mapper.readValue(downloadFiles, new TypeReference<Map<String, Boolean>>() {});

			for (Entry<String, Boolean> entry : nodes.entrySet()) {	        		
	        	srcFiles.add(entry.getKey());
			}
		}
	    catch (Exception e) {
	    }
                 
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        
        try {
             
            // create byte buffer
            byte[] buffer = new byte[1024];
 
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
             
            for (int i=0; i < srcFiles.size(); i++) {
                 
                File srcFile = new File(srcFiles.get(i));
 
                FileInputStream fis = new FileInputStream(srcFile);
 
                // begin writing a new ZIP entry, positions the stream to the start of the entry data
                zos.putNextEntry(new ZipEntry(srcFile.getName()));
                 
                int length;
 
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
 
                zos.closeEntry();
 
                // close the InputStream
                fis.close();
            }
 
            // close the ZipOutputStream
            zos.close();
        }
        catch (IOException ioe) {
            System.out.println("Error creating zip file: " + ioe);
        }
		return zos;
	}
	
	public String downloadDocumentByPath(Session session, String nodePath){
	   Document newDocument =  (Document) session.getObjectByPath(nodePath);
	   String downloadsFolder = null;
	   logger.debug("Downloading file: " + nodePath);

        try {
        	downloadsFolder = System.getProperty("java.io.tmpdir");
			FileUtils.download(newDocument, downloadsFolder + "/" + newDocument.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return downloadsFolder + "/" + newDocument.getName();
	}
	
	public void createFolder(Session session, String path, String folderName) {
		// set folder properties
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(PropertyIds.NAME, folderName);
		properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
		
		// get parent folder object
		ObjectId parentFolderId = session.getObjectByPath(path);
		
		// check the folder doesn't already exist
		String folderPath = path + "/" + folderName;
		
		try {
			session.getObjectByPath(folderPath);
		} catch (CmisObjectNotFoundException conf) {
			// create the folder
			((Folder) parentFolderId).createFolder(properties);
			logger.debug("Folder " + folderName + " created");
		}
	}
	
	public void deleteByPath(Session session, String objectName) {
		CmisObject cmisObject = session.getObjectByPath(objectName);
		if (cmisObject instanceof Document) {
			boolean isCheckedOut = Boolean.TRUE.equals(((DocumentProperties) cmisObject).isVersionSeriesCheckedOut());
			if (!isCheckedOut) {
				cmisObject.delete(true);
			}
		} else if (cmisObject instanceof Folder) {
			((Folder) cmisObject).deleteTree(true, UnfileObject.DELETE, true);
		}
	}
	
	public void deleteById(Session session, String nodePath) {
		CmisObject cmisObject = session.getObjectByPath(nodePath);
		if (cmisObject instanceof Document) {
			boolean isCheckedOut = Boolean.TRUE.equals(((DocumentProperties) cmisObject).isVersionSeriesCheckedOut());
			if (!isCheckedOut) {
				cmisObject.delete(true);
			}
		} else if (cmisObject instanceof Folder) {
			((Folder) cmisObject).deleteTree(true, UnfileObject.DELETE, true);
		}
	}
	
	public void pasteFilesByPath(Session session, String objectName, String sourceFolder, String destinationFolder, String action) {
		CmisObject node = session.getObjectByPath(objectName);
        Folder destFolder = (Folder)(session.getObjectByPath(destinationFolder));
        
		
        if (action.equals("copy")){
			if (node instanceof Document) {
				boolean isCheckedOut = Boolean.TRUE.equals(((DocumentProperties) node).isVersionSeriesCheckedOut());
				if (!isCheckedOut) {
					try {
						Map<String, Object> properties = new HashMap<String, Object>();
						List<Property<?>> props = node.getProperties();
						for (Property<?> p : props) {
							if (p.getValueAsString()!= null) {
								properties.put(p.getId(), p.getValue());
							}
						}
						((Document) node).copy(destFolder, properties, null, null, null, null, null);
					} catch (Exception e) {
						logger.debug("Error copying content: " + node.getId() + " : " + node.getName() + " msg: " + e.getMessage());
					}
				}
			} else if (node instanceof Folder) {
				try {
					copyFolder(destFolder, (Folder) node);
				} catch (Exception e) {
					logger.debug("Error copying content: " + node.getId() + " : " + node.getName() + " msg: " + e.getMessage());
				}
			}
		} else if (action.equals("move")){
			logger.debug("moving file...");
			if (node instanceof Document) {
				boolean isCheckedOut = Boolean.TRUE.equals(((DocumentProperties) node).isVersionSeriesCheckedOut());
				if (!isCheckedOut) {
					try {
						((Document) node).move(((FileableCmisObject) node).getParents().get(0), destFolder);
					} catch (Exception e) {
						logger.debug("Error moving content: " + node.getId() + " : " + node.getName() + " msg: " + e.getMessage());
					}
				}
			} else if (node instanceof Folder) {
				copyFolder(destFolder, (Folder) node);
			}
		}
	}
	
	public void copyFolder(Folder destinationFolder, Folder toCopyFolder) {
		Map<String, Object> folderProperties =	new HashMap<String, Object>();
		folderProperties.put(PropertyIds.NAME, toCopyFolder.getName());
		folderProperties.put(PropertyIds.OBJECT_TYPE_ID,
		toCopyFolder.getBaseTypeId().value());
		Folder newFolder = destinationFolder.createFolder(folderProperties);
		copyChildren(newFolder, toCopyFolder);
	}
	
	public void copyChildren(Folder destinationFolder, Folder toCopyFolder) {
		ItemIterable<CmisObject> immediateChildren =
		toCopyFolder.getChildren();
		for (CmisObject child : immediateChildren) {
			if (child instanceof Document) {
				((Document) child).copy(destinationFolder);
			} else if (child instanceof Folder) {
				copyFolder(destinationFolder, (Folder) child);
			}
		}
	}
	
	/*
	public void moveDocument(Session session, String objectName, String pasteFilesPath) {
		Folder parentFolder = session.getRootFolder();
		Folder sourceFolder = getDocumentParentFolder(document);
		String destinationFolderName = "User Homes";
		Folder destinationFolder = (Folder) getObject(
		session, parentFolder, destinationFolderName);
		// Check that we got the document, then move
		if (document != null) {
		// Make sure the user is allowed to move the document
		// to a new folder
		if (document.getAllowableActions().getAllowableActions().
		contains(Action.CAN_MOVE_OBJECT) == false) {
		throw new CmisUnauthorizedException("Current user does" +
		" not have permission to move " +
		getDocumentPath(document) + document.getName());
		}
		String pathBeforeMove = getDocumentPath(document);
		try {
		document.move(sourceFolder, destinationFolder);
		logger.info("Moved document " + pathBeforeMove +
		" to folder " + destinationFolder.getPath());
		} catch (CmisRuntimeException e) {
		logger.error("Cannot move document to folder " +
		destinationFolder.getPath() + ": " + e.getMessage());
		}
		} else {
		logger.error("Document is null, cannot move!");
		}
	}
	*/
	
	public void uploadToPath(Session session, String path, MultipartFile[] uploadFiles ) {
		String targetFolder = path;

        for(MultipartFile uploadedFile : uploadFiles) {
            logger.debug("Uploading file: " + targetFolder + "/" + uploadedFile.getOriginalFilename());
            
            // Get the file and save it on the server
            byte[] bytes = null;
			try {
				bytes = uploadedFile.getBytes();
			} catch (IOException e) {
				e.printStackTrace();
			}
            Path tempPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + uploadedFile.getOriginalFilename());
            
            try {
				Files.write(tempPath, bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
            
            // Upload file from server to repository
            File file = new File(System.getProperty("java.io.tmpdir") +  "/" + uploadedFile.getOriginalFilename());
            byte[] fileContent = null;
			try {
				fileContent = readFile(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
            Folder parent = (Folder)(session.getObjectByPath(path));
            String targetPath = (path + "/" + file.getName()).replaceAll("/+", "/");

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            properties.put(PropertyIds.NAME, file.getName());
            ContentStream contentStream = new ContentStreamImpl(
                file.getName(),
                BigInteger.valueOf(fileContent.length),
                "binary/octet-stream",
                new ByteArrayInputStream(fileContent));
            
            // Remember to delete the temporary file
            file.delete();
            
            // If the file exists check it out first
            try {
            	CmisObject cmisObject = session.getObjectByPath(targetPath);
	    		boolean isCheckedOut = Boolean.TRUE.equals(((DocumentProperties) cmisObject).isVersionSeriesCheckedOut());
	    		
	    		if (isCheckedOut == false) {
	    			ObjectId pwcId = ((Document) cmisObject).checkOut();
	    			Document pwc = (Document) session.getObject(pwcId);
	                pwc.checkIn(false, properties, contentStream, "");
	    		}
            } catch (CmisObjectNotFoundException onfe) {
                parent.createDocument(properties, contentStream, VersioningState.MAJOR);
            }
        }

	}
	
	public List <CMISObject> getObjectsByPath(Session session, String root_folder, String path, String back){
    	List <CMISObject> objects = new ArrayList<CMISObject>();
		CMISObject node = new CMISObject();

		Folder parentFolder = null;

		try {
			/* Load list of objects to be displayed */
			if (path == null) {
				parentFolder = (Folder) session.getObjectByPath(root_folder);
			} else {
				if (!path.startsWith(root_folder + "/") && !path.equals(root_folder)) {
					// Generate Error
				} else {
					currentPath = path;
					parentFolder = (Folder) session.getObjectByPath(path);		
				}
			} 

	        for (Iterator<CmisObject> it = (parentFolder.getChildren().iterator()); it.hasNext();) {
				// For each object get the properties
				CmisObject o = it.next();
				node.setId(o.getId());
				node.setNodeRef(o.getPropertyValue("alfcmis:nodeRef").toString());
				node.setName(o.getPropertyValue("cmis:name").toString());
				node.setBaseTypeId(o.getPropertyValue("cmis:baseTypeId").toString());
				node.setCreatedBy(o.getPropertyValue("cmis:createdBy").toString());
				node.setLastModifiedBy(o.getPropertyValue("cmis:lastModifiedBy").toString());
				
				if (o.getPropertyValue("cmis:versionLabel") != null) {
					node.setVersion(o.getPropertyValue("cmis:versionLabel").toString());
				}
				
				// Convert modification date to string
		        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");	
		        Calendar calendar = o.getPropertyValue("cmis:lastModificationDate");
				node.setLastModificationDate(sdf.format(calendar.getTime()));
				Action canDelete = Action.CAN_DELETE_OBJECT;
				if (o.getAllowableActions().getAllowableActions().contains(canDelete)) {
				       node.setCanDelete(true); 
				}
				 
				if (BaseTypeId.CMIS_DOCUMENT.equals(o.getBaseTypeId())) {
					Integer size = Integer.valueOf(o.getPropertyValue("cmis:contentStreamLength").toString()) / 1000;
					node.setSize(size);
	
					if (o.getPropertyValue("cmis:parentId") != null) {
						session.getObject(o.getPropertyValue("cmis:parentId").toString());
						CmisObject parent = session.getObject((ObjectId) o.getPropertyValue("cmis:parentId"));
						node.setPath(parent.getPropertyValue("cmis:path").toString());
					} else {
						node.setPath(root_folder);
					};
				} else if (BaseTypeId.CMIS_FOLDER.equals(o.getBaseTypeId())) {
					node.setPath(o.getPropertyValue("cmis:path").toString()); 
				}
				
				objects.add(node);
				node = new CMISObject();
			}
		} catch (Exception e) {
			logger.debug("Parent folder not found: " + path);
		}

		return objects;
	}
	
	public boolean canCreate(Session session, String path) {
		CmisObject cmisObject = session.getObjectByPath(path);
		boolean create = false;
		
		if (cmisObject.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_DOCUMENT) || cmisObject.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_FOLDER)) {
		      create = true; 
		}
		return create;
	}
	
	public String getParent(Session session, String path) {
		CmisObject cmisObject = session.getObjectByPath(path);
		String parentId = cmisObject.getProperty("cmis:parentId").getFirstValue().toString();
		CmisObject parent = session.getObject(parentId);
		return parent.getProperty("cmis:path").getFirstValue().toString();
	}
	
    /**
     * Utility method for reading a file's content.
     */
    private byte[] readFile(File file) throws IOException {

        byte[] fileContent = new byte[(int)file.length()];
        FileInputStream istream = new FileInputStream(file);
        istream.read(fileContent);
        istream.close();
        return fileContent;
    }
}