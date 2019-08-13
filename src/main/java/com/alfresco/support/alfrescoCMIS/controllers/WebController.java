package com.alfresco.support.alfrescoCMIS.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import com.alfresco.support.alfrescoCMIS.model.CMISSession;
import com.alfresco.support.alfrescoCMIS.model.Login;
import com.alfresco.support.alfrescoCMIS.model.Search;
import com.alfresco.support.alfrescoCMIS.model.CMISObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@ControllerAdvice
@SessionScope
public class WebController implements ErrorController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${alfresco_atompub_url}")
    private String alfresco_atompub_url;

    @Value("${repository_id}")
    private String repository_id;
    
    @Value("${root_folder}")
    private String root_folder;
    
    private CMISSession cmisSession = new CMISSession();

    private String user = null;
   
    @Autowired
    private ServletContext servletContext;
    
    /**
     * If valid session redirect to acs navigation page otherwise go back to login page
     * @param response
     */
    @RequestMapping("/")
    public void index(HttpServletResponse response) {
        try {
        	if (cmisSession.getSession() == null) {
        		response.sendRedirect("login");
        	} else {
    			response.sendRedirect("acs");
        	}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Show login page
     * @param model
     * @return login view
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("login", new Login());
        //go to login page
        return "login";        
    }

    /**
     * Process login request. If user is authenticated redirect to acs navigation page
     * @param model
     * @param login
     * @param response
     */
    @PostMapping("/login")
    public void login(Model model, @ModelAttribute Login login, HttpServletResponse response) {
    	logger.debug("Authentication details - username: " + login.getUsername());
    	cmisSession = new CMISSession();
      	cmisSession.connect(alfresco_atompub_url, repository_id, login.getUsername(), login.getPassword());

        try {
        	user = login.getUsername();
			response.sendRedirect("acs");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * Terminate the session and redirect to login page
     * @param response
     */
    @RequestMapping("/logout")
    public void logout(HttpServletResponse response) {
		// go to login page if session is null, otherwise go to acs navigation
    	cmisSession = null;
        try {
        	response.sendRedirect("login");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * Main repository navigation page
     * @param model
     * @param login
     * @param response
     * @param request
     * @param path
     * @param navigateTo
     * @param back
     * @param delete
     * @param folderName
     * @param downloadFiles
     * @param deleteFiles
     * @param uploadFiles
     * @param uploadFiles2
     * @return
     * @throws Exception
     */
    @RequestMapping("/acs")
    public String acs(Model model, 
    		@ModelAttribute Login login, 
    		HttpServletResponse response, 
    		HttpServletRequest request, 
    		@RequestParam(required = false) String path, 
    		@RequestParam(required = false) String navigateTo, 
    		@RequestParam(required = false) String back,
    		@RequestParam(required = false) String delete,
    		@RequestParam(required = false, name = "folderName") String folderName,
    		@RequestParam(required = false) String downloadFiles,
    		@RequestParam(required = false) String deleteFiles,
    		@RequestParam(required = false) String sourceFolder,
    		@RequestParam(required = false) String destinationFolder,
    		@RequestParam(required = false) String pasteFiles,
    		@RequestParam(required = false) String pasteAction,
    		//@RequestParam(required = false) String copyFiles,
    		//@RequestParam(required = false) String moveFiles,
    		//@RequestParam(required = false) String pasteFilesPath,
    		@RequestParam(required=false) MultipartFile[] uploadFiles, 
    		@RequestParam(required=false) MultipartFile[] uploadFiles2) throws Exception {
		
    	logger.debug("Path: " + path);
    	logger.debug("back: " + back);
    	logger.debug("delete: " + delete);
    	logger.debug("folderName: " + folderName);
    	logger.debug("downloadFiles: " + downloadFiles);
    	logger.debug("deleteFiles: " + deleteFiles);
    	logger.debug("sourceFolder: " + sourceFolder);
    	logger.debug("destinationFolder: " + destinationFolder);
    	logger.debug("pasteFiles: " + pasteFiles);
    	logger.debug("pasteAction: " + pasteAction);

    	// list of objects to be passed to the model in the html template
    	List <CMISObject> objects = new ArrayList<CMISObject>();
    	// Current path to be passed back to the model
		String currentPath = root_folder;

		
		/* Paste files  */
		if ((sourceFolder != null && (sourceFolder.startsWith(root_folder + "/") || sourceFolder.equals(root_folder) || sourceFolder.equals("NA"))) &&
			(destinationFolder != null && (destinationFolder.startsWith(root_folder + "/") || destinationFolder.equals(root_folder)))) {
	        // using for-each loop for iteration over Map.entrySet() 
	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Boolean> nodes = mapper.readValue(pasteFiles, new TypeReference<Map<String, Boolean>>() {
            });
	        
	        for (Entry<String, Boolean> entry : nodes.entrySet()) {
	        	if (entry.getValue()) {
	        		if (pasteFiles != null) {
		        		cmisSession.pasteFilesByPath(cmisSession.getSession(), entry.getKey(), sourceFolder, destinationFolder, pasteAction);	        			
	        		}
	        	}
	        }
	        path = destinationFolder;
		}
		
		HttpSession session = request.getSession(false);
		if (navigateTo == null) {
			navigateTo = (String) session.getAttribute("navigateTo");
		}
	
		if (navigateTo != null) {
			path = navigateTo;
			session.removeAttribute("navigateTo");
		} else if (path == null) {
			path = root_folder;
		}
		
		/* Delete files from table multi row selection */
		if (deleteFiles != null && (path.startsWith(root_folder + "/") || path.equals(root_folder))) {
	        // using for-each loop for iteration over Map.entrySet() 
	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Boolean> nodes = mapper.readValue(deleteFiles, new TypeReference<Map<String, Boolean>>() {
            });
	        
	        for (Entry<String, Boolean> entry : nodes.entrySet()) {
	        	
	        	if (entry.getValue()) {
	        		cmisSession.deleteByPath(cmisSession.getSession(), entry.getKey());
	        	}
	        }
		}
		
		/* Create a new folder */
		if (folderName != null && (path.startsWith(root_folder + "/") || path.equals(root_folder))) {
			cmisSession.createFolder(cmisSession.getSession(), path, folderName);
		}

		/* Upload content */
		if((uploadFiles != null && uploadFiles.length > 0) && (path.startsWith(root_folder + "/") || path.equals(root_folder))) {
			cmisSession.uploadToPath(cmisSession.getSession(), path, uploadFiles);
		} else if((uploadFiles2 != null && uploadFiles2.length > 0) && (path.startsWith(root_folder + "/") || path.equals(root_folder))) {
			cmisSession.uploadToPath(cmisSession.getSession(), path, uploadFiles2);
		}
		
		/* Download content */
		if (downloadFiles != null && downloadFiles.startsWith(root_folder + "/")) {
			session = request.getSession(false);
			session.setAttribute("downloadFiles", downloadFiles);
			try {
	        	response.sendRedirect("download");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/* Delete content */
		if (delete != null && delete.startsWith(root_folder + "/")) {
			cmisSession.deleteByPath(cmisSession.getSession(), delete);
		}

		/* Get objects for ACS view */
		if (path == null) {
			path = root_folder;
		} else if (back != null && back.equals("true") && !path.equals(root_folder)) {
			//path = path.substring(0, path.lastIndexOf("/"));
	    	path = cmisSession.getParent(cmisSession.getSession(), path);
		}
		
		objects = cmisSession.getObjectsByPath(cmisSession.getSession(), root_folder, path, back);

        // Check if user has permissions to write to current path to enable/disable actions
        boolean canCreate = cmisSession.canCreate(cmisSession.getSession(), currentPath);
        
        // Add parameters to model to be used in html view
		model.addAttribute("objects", objects);
		model.addAttribute("path", path);	
		model.addAttribute("canCreate", canCreate);	
		model.addAttribute("user", user);	
        model.addAttribute("search", new Search());

		return "acs";
    }
    
    /**
     * Search request
     * @param model
     * @param response
     * @param request
     * @param search
     * @param searchTermDownload
     * @param navigateTo
     * @param searchTermDelete
     * @param deleteFiles
     * @param path
     * @param downloadFiles
     * @return
     */
    @RequestMapping("/search")
    public String search(Model model,
    		HttpServletResponse response, 
    		HttpServletRequest request, 
    		@ModelAttribute Search search, 
    		@RequestParam(required = false) String searchTermDownload, 
    		@RequestParam(required = false) String navigateTo, 
    		@RequestParam(required = false) String searchTermDelete, 
    		@RequestParam(required = false) String deleteFiles, 
    		@RequestParam(required = false) String path, 
    		@RequestParam(required = false) String downloadFiles){
    	
    	logger.debug("search term download " + searchTermDownload); 
    	logger.debug("search term delete " + searchTermDownload); 
    	logger.debug("term " + search.getTerm()); 
    	logger.debug("delete " + deleteFiles); 
    	logger.debug("navigate to " + navigateTo); 
    	logger.debug("deleteFiles " + deleteFiles); 
    	
    	if (searchTermDownload != "" && searchTermDownload != null) {
    		search.setTerm(searchTermDownload);
    	} else if (searchTermDelete != "" && searchTermDelete != null) {
    		search.setTerm(searchTermDelete);
    	}
    	
    	String term = search.getTerm();
    	
		try {
			if (navigateTo != null && !navigateTo.equals("")) {
				HttpSession session = request.getSession(false);
				session.setAttribute("navigateTo", navigateTo.substring(0,navigateTo.lastIndexOf("/")));
				response.sendRedirect("acs");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/* Download content */
		if (downloadFiles != null && downloadFiles.startsWith(root_folder + "/")) {
			HttpSession session = request.getSession(false);
			session.setAttribute("downloadFiles", downloadFiles);
			try {
	        	response.sendRedirect("download");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/* Delete files from table multi row selection */
		if (deleteFiles != null) {
	        // using for-each loop for iteration over Map.entrySet() 
	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Boolean> nodes;
			try {
				nodes = mapper.readValue(deleteFiles, new TypeReference<Map<String, Boolean>>() {});

				for (Entry<String, Boolean> entry : nodes.entrySet()) {
		        	if (entry.getValue()) {
		        		cmisSession.deleteByPath(cmisSession.getSession(), entry.getKey());
		        	}
		        }
			}
		    catch (Exception e) {
		    }
		}
		List <CMISObject> objects = new ArrayList<CMISObject>();

      	objects = cmisSession.search(cmisSession.getSession(), root_folder, term);
    	
        // Add parameters to model to be used in html view
		model.addAttribute("objects", objects);
		model.addAttribute("user", user);	

		return "search";
    }
    
    /**
     * Download file to server and then push it down to client
     * @return ResponseEntity
     * @param request
     * @param response
     * @throws IOException
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
    		HttpServletRequest request,
    		HttpServletResponse response
    		) throws IOException {
        HttpSession session=request.getSession(false);
        String downloadFiles = (String) session.getAttribute("downloadFiles");
		String localFile = cmisSession.downloadDocumentByPath(cmisSession.getSession(), downloadFiles);
    	File file = new File(localFile);
    	InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    	MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, localFile);
    	return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_DISPOSITION,
               "attachment;filename=" + file.getName())
         .contentType(mediaType).contentLength(file.length())
         .body(resource);
    }
   
    /**
     * Download file to server and then push it down to client
     * @return ResponseEntity
     * @param request
     * @param response
     * @throws IOException
     */
    @GetMapping("/downloadMultipleFiles")
    public ResponseEntity<ZipOutputStream> downloadMultipleFiles(
    		HttpServletRequest request,
    		HttpServletResponse response
    		) throws IOException {
        HttpSession session=request.getSession(false);
        String downloadFiles = (String) session.getAttribute("downloadFiles");
		String localFile = cmisSession.downloadDocumentByPath(cmisSession.getSession(), downloadFiles);
    	//InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    	
    	ZipOutputStream file  = cmisSession.downloadMultipleFiles(downloadFiles);
    	
    	MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, localFile);
    	return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_DISPOSITION,
               "attachment;filename=content.zip")
         .contentType(mediaType)
         .body(file);
    }
    
    /**
     *  Error pages for generic HTTP errors
     * @param response
     * @return ModelAndView
     */
	@RequestMapping("/error")
	public ModelAndView handleError(HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView();

		if(response.getStatus() == HttpStatus.NOT_FOUND.value()) {
			modelAndView.addObject("message", "Message: Page not found");
			modelAndView.setViewName("error-404");
		}
		else if(response.getStatus() == HttpStatus.FORBIDDEN.value()) {
			modelAndView.addObject("message", "Message: Unauthorised access");
			modelAndView.setViewName("error-403");
		}
		else if(response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
			modelAndView.addObject("message", "Message: System error");
			modelAndView.setViewName("error-500");
		}
		else {
			modelAndView.addObject("message", "Message: An unexpected error has occurred");
			modelAndView.setViewName("error");
		}
		// go to specific error page
		return modelAndView;
	}


	@Override
	public String getErrorPath() {
	    return "/error";
	}
}