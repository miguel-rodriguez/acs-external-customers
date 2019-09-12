var input = document.querySelector('input[type=file]');
var selected = 0;
var selectedEntry = "";

// Function to check if file ends with any specified extension
function endsWithAny(suffixes, string) {
    return suffixes.some(function (suffix) {
        return string.endsWith(suffix);
    });
}

// things to do when the page is loaded
$(document).ready(function(){
	// disable pasting if there are no files selected
	console.log(sessionStorage.getItem("files"));
	if ((sessionStorage.getItem("files") === "null" || sessionStorage.getItem("files") == null) && canCreate==true) {
		if (document.getElementById("pasteNodesImage") !== null){
			document.getElementById("pasteNodesImage").style.display = 'none';
		}
	} else {
		if (document.getElementById("pasteNodesImage") !== null){
			document.getElementById("pasteNodesImage").style.display = 'inline';
		}
	}
	
	// set the focus in the create folder modal window
    $("#createFolderWindow").on('shown.bs.modal', function(){
        $(this).find('input[type="text"]').focus();
    });

    // display any alert messages sent to this page
    var message = document.getElementById("message").getAttribute('value');
    if (message != null && message !== ""){
        $.notify({
        	// options
        	message: message,
        },{
        	// settings
        	type: 'danger',
        	delay: 0,
        	animate: {
        		enter: 'animated fadeInDown',
        		exit: 'animated fadeOutUp'
        	},
        	placement: { 
        		from: 'top',
        		align: 'center',
        	},
        });
    }
});

window.addEventListener("dragover",function(e){
	  e = e || event;
	  e.preventDefault();
	},false);

$('#uploadFiles2').change(function() {
    $('#dragAndDrop').submit();
});

// Copy nodes
function copyNodes(path) {
    //document.getElementById("copyFiles").value = JSON.stringify(el);
  	sessionStorage.setItem("files", JSON.stringify(el));    
  	sessionStorage.setItem("action", "copy");
  	sessionStorage.setItem("sourceFolder", path);

    $.notify({
    	// options
    	message: 'Ready to copy...paste your file(s) in the destination folder.' 
    },{
    	// settings
    	delay: 3000,
    	animate: {
    		enter: 'animated fadeInDown',
    		exit: 'animated fadeOutUp'
    	},
    	placement: { 
    		from: 'top',
    		align: 'center',
    	},
    });
}

$("#copyNodesImage").click(function () {
    document.getElementById("pasteNodesImage").style.display = 'inline';
});


//Move nodes
function moveNodes(path) {
    //document.getElementById("moveFiles").value = JSON.stringify(el);
  	sessionStorage.setItem("files", JSON.stringify(el));    
    sessionStorage.setItem("action", "move");
  	sessionStorage.setItem("sourceFolder", path);

    $.notify({
    	// options
    	message: 'Ready to move...paste your file(s) in the destination folder.' 
    },
    {
    	// settings
    	delay: 3000,
    	animate: {
    		enter: 'animated fadeInDown',
    		exit: 'animated fadeOutUp'
    	},
    	placement: { 
    		from: 'top',
    		align: 'center',
    	},
    });
}

$("#moveNodesImage").click(function () {
    document.getElementById("pasteNodesImage").style.display = 'inline';
});

// Paste nodes
function pasteNodes(path) {
    document.getElementById("sourceFolder").value = sessionStorage.getItem("sourceFolder");
    document.getElementById("destinationFolder").value = path;
    document.getElementById("pasteFiles").value = sessionStorage.getItem("files");
    document.getElementById("pasteAction").value = sessionStorage.getItem("action");

    //alert("paste path: " + path + " files: " + sessionStorage.getItem("files") + " action: " + sessionStorage.getItem("action"));
    $.notify({
    	// options
    	message: 'Content pasted.' 
    },{
    	// settings
    	delay: 3000,
    	animate: {
    		enter: 'animated fadeInDown',
    		exit: 'animated fadeOutUp'
    	},
    	placement: { 
    		from: 'top',
    		align: 'center',
    	},
    });
    
    
    setTimeout(function(){
        document.getElementById("pasteFilesForm").submit();
    	sessionStorage.removeItem("files");
        document.getElementById("pasteNodesImage").style.display = 'none';
    }, 2000); 
}

//Create folder pop up window and form submission
function createFolder() {
    $('#createFolderWindow').modal('show');
}

// Create Folder
function submitCreateFolder(action, folderName) {
    $('#createFolderWindow').modal('hide');
    if (action == true) {
        document.getElementById("folderName").value = folderName;
        document.getElementById("createFolderForm").submit();
    }
};

// Delete Nodes
function confirmDeleteNodes() {
    $('#deleteFileWindow').modal('show');
}

function deleteNodes() {
    $('#deleteFileWindow').modal('hide');
    if (action == true) {
        document.getElementById("deleteFiles").value = JSON.stringify(el);
        document.getElementById("deleteFilesForm").submit();
    }
};

// Download nodes
function downloadNodes(fileName, path) {
	let url = this.url;

	document.getElementById("downloadFiles").value = fileName;
    document.getElementById("downloadFilesForm").submit();
};

//Preview file
function previewFile() {
		window.open('/preview?fileName=' + selectedEntry, '');
};

// Navigate to when clicking on folder url
function navigateTo(navigateTo, back) {
    document.getElementById("navigateTo").value = navigateTo;
    document.getElementById("back").value = back;
    document.getElementById("navigateToForm").submit();
};

// Upload 
$("#upload").click(function () {
    $("#uploadFiles2").trigger('click');
});

function uploadContent() {
    document.getElementById("dragAndDrop").submit();
}

// DnD container
function dragstartHandler(evt){
      evt.preventDefault();
      element = document.getElementById("dropContainer");
	  event.originalEvent.dataTransfer.setData('text/plain', 'anything');
}

document.ondragover = dropContainer.ondragenter = function(evt) {
    evt.preventDefault();
    element = document.getElementById("dropContainer");
    element.setAttribute("style","border: 4px dotted green; position: fixed; top: 0px; left: 0px; width: 100%; height: 100%; z-index: 100;");
};

dropContainer.ondragover = dropContainer.ondragenter = function(evt) {
    evt.preventDefault();
};

dropContainer.ondragleave =function(evt) {
    evt.preventDefault();
    element = document.getElementById("dropContainer");
    element.setAttribute("style", "z-index: 0;");
};

dropContainer.ondrop = function(evt) {
    if(evt.preventDefault) { evt.preventDefault(); }
    if(evt.stopPropagation) { evt.stopPropagation(); }
    element = document.getElementById("dropContainer");
    element.setAttribute("style","");
    
    isDirectory = false
    var items = evt.dataTransfer.items;
    for (var i=0; i<items.length; i++) {
	  var item = items[i].webkitGetAsEntry();
	  if (item.isDirectory) {
	      isDirectory = true;
      } 
    }   
    
    if (isDirectory == false) {
 	    uploadFiles2.files = evt.dataTransfer.files;
 	    document.getElementById("dragAndDrop").submit();
    } else {
        $.notify({
        	// options
        	message: 'Can\'t drag & drop folders.' 
        },{
        	// settings
        	delay: 3000,
        	type: 'danger',        	
        	animate: {
        		enter: 'animated fadeInDown',
        		exit: 'animated fadeOutUp'
        	},
        	placement: { 
        		from: 'top',
        		align: 'center',
        	},
        });
    }
};

// Row selection
var el = {};
var canDelete = {};
$(document).ready(function() {
    var events = $('#events');
    var table = $('#data').DataTable({
        select: true,
        "order": [5, 'desc'],
        "oLanguage": {
            "sEmptyTable": "No content found"
        },
        "oLanguage": {
            "sSearch": "Filter:"
        },
        scrollCollapse: true,
        scroller:       true,
    });
    // process select / deselect row action
    table
    .on('select', function(e, dt, type, indexes) {
    	if (canCreate==true) {
    		document.getElementById("copyNodesImage").style.display = 'inline';
    		document.getElementById("moveNodesImage").style.display = 'inline';
    		document.getElementById("deleteNodesImage").style.display = 'inline';
    	}

        var rowData = table.rows(indexes).data().toArray();
        selectedEntry = rowData[0][0];

        // add selected elements and permission
        for (i = 0; i < rowData.length; i++) {
            el[rowData[i][0]] = "true";
            canDelete[rowData[i][0]] = rowData[i][1];
            selected++;
        }
        
        // Allow preview if file ends with specific extension
        if (endsWithAny([".pdf", ".jpg", ".txt", ".png"], selectedEntry) && selected==1){
        	document.getElementById("previewFile").style.display = 'inline';
        } else {
        	document.getElementById("previewFile").style.display = 'none';
        }

        // iterate all selected elements and check if permission to delete is enabled
        Object.keys(el).forEach(function(key) {
            if (el[key] === "true" && canDelete[key] === "false") {
            	if (canCreate==true) {
            		document.getElementById("deleteNodesImage").style.display = 'none';
            	}
            }
        });
    })
	.on('deselect', function(e, dt, type, indexes) {
        var rowData = table.rows(indexes).data().toArray();
        document.getElementById("previewFile").style.display = 'inline';
        if (canCreate) {
            document.getElementById("deleteNodesImage").style.display = 'inline';
            document.getElementById("copyNodesImage").style.display = 'inline';
            document.getElementById("moveNodesImage").style.display = 'inline';
        }
        
        for (i = 0; i < rowData.length; i++) {
            el[rowData[i][0]] = "false";
            canDelete[rowData[i][0]] = rowData[i][1];

            selected--;

            document.getElementById("previewFile").style.display = 'none';
            if (selected == 0) {
            	if (canCreate==true) {
                    document.getElementById("deleteNodesImage").style.display = 'none';
                    document.getElementById("copyNodesImage").style.display = 'none';
                    document.getElementById("moveNodesImage").style.display = 'none';
            	}
            }
        }

        Object.keys(el).forEach(function(key) {
            if (el[key] === "true" && canDelete[key] === "false") {
                if (canCreate==true) {
                	document.getElementById("deleteNodesImage").style.display = 'none';
                }
            }
        });
    });
});

