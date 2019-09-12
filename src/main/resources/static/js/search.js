var input = document.querySelector('input[type=file]');
var selected = 0;
var el = {};
var canDelete = {};
var selectedEntry = "";

// Upload form
/*
input.onchange = function() {
    $("#uploadFiles2").trigger('click');
};
*/

//Function to check if file ends with any specified extension
function endsWithAny(suffixes, string) {
    return suffixes.some(function (suffix) {
        return string.endsWith(suffix);
    });
}

$("#upload").click(function() {
    $("#uploadFiles2").trigger('click');
});

$('#uploadFiles2').change(function() {
    $('#dragAndDrop').submit();
});

//Create folder pop up window and form submission
function createFolder() {
    $('#createFolderWindow').modal('show');
}

$(document).ready(function(){
    $("#createFolderWindow").on('shown.bs.modal', function(){
        $(this).find('input[type="text"]').focus();
    });
});

function submitCreateFolder(action, folderName) {
    $('#createFolderWindow').modal('hide');
    if (action == true) {
        document.getElementById("folderName").value = folderName;
        document.getElementById("createFolderForm").submit();
    }
};

function confirmDeleteNodes(singleDelete) {
    $('#deleteFileWindow').modal('show');
}

function deleteNodes() {
    $('#deleteFileWindow').modal('hide');
    if (action == true) {
        document.getElementById("deleteFiles").value = JSON.stringify(el);
    	document.getElementById("searchTermDelete").value = document.getElementById("term").value;
        document.getElementById("deleteFilesForm").submit();
    }
};

function downloadNodes(fileName, path) {
    document.getElementById("downloadFiles").value = fileName;
    
	if (document.getElementById("searchTermDownload").value){
    	document.getElementById("searchTermDownload").value = document.getElementById("searchTermDownload").value;
    } else if (document.getElementById("term").value){
    	document.getElementById("searchTermDownload").value = document.getElementById("term").value;
    }
    document.getElementById("downloadFilesForm").submit();
};

function navigateTo(navigateTo, back) {
    document.getElementById("navigateTo").value = navigateTo;
    document.getElementById("back").value = back;
    document.getElementById("navigateToForm").submit();
};

//Copy nodes
function copyNodes(path) {
    //document.getElementById("copyFiles").value = JSON.stringify(el);
  	sessionStorage.setItem("files", JSON.stringify(el));    
  	sessionStorage.setItem("action", "copy");
  	sessionStorage.setItem("sourceFolder", "NA");

    $.notify({
    	// options
    	message: 'Ready to copy...paste your file(s) in the destination folder.' 
    },{
    	// settings
    	delay: 1000,
    	animate: {
    		enter: 'animated fadeInDown',
    		exit: 'animated fadeOutUp'
    	},
    });
}

//Move nodes
function moveNodes(path) {
    //document.getElementById("moveFiles").value = JSON.stringify(el);
  	sessionStorage.setItem("files", JSON.stringify(el));    
    sessionStorage.setItem("action", "move");
  	sessionStorage.setItem("sourceFolder", "NA");

    $.notify({
    	// options
    	message: 'Ready to move...paste your file(s) in the destination folder.' 
    },{
    	// settings
    	delay: '1000',
    	animate: {
    		enter: 'animated fadeInDown',
    		exit: 'animated fadeOutUp'
    	},
    });
}

//Preview file
function previewFile() {
		window.open('/preview?fileName=' + selectedEntry, '');
};

//Row selection
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
    });
    // process select / deselect row action
    table
        .on('deselect', function(e, dt, type, indexes) {
            var rowData = table.rows(indexes).data().toArray();
            document.getElementById("previewFile").style.display = 'inline';

            document.getElementById("deleteNodesImage").style.display = 'inline';
            document.getElementById("copyNodesImage").style.display = 'inline';
            document.getElementById("moveNodesImage").style.display = 'inline';

            for (i = 0; i < rowData.length; i++) {
                el[rowData[i][0]] = "false";
                canDelete[rowData[i][0]] = rowData[i][1];

                selected--;
                
                document.getElementById("previewFile").style.display = 'none';
                if (selected == 0) {
                    document.getElementById("deleteNodesImage").style.display = 'none';
                    document.getElementById("copyNodesImage").style.display = 'none';
                    document.getElementById("moveNodesImage").style.display = 'none';                      
                }
            }

            Object.keys(el).forEach(function(key) {
                if (el[key] === "true" && canDelete[key] === "false") {
                    document.getElementById("deleteNodesImage").style.display = 'none';
                    document.getElementById("copyNodesImage").style.display = 'none';
                    document.getElementById("moveNodesImage").style.display = 'none';                      
                }
            });
        })
        .on('select', function(e, dt, type, indexes) {
            document.getElementById("deleteNodesImage").style.display = 'inline';
            document.getElementById("copyNodesImage").style.display = 'inline';
            document.getElementById("moveNodesImage").style.display = 'inline';            
            
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
                    document.getElementById("deleteNodesImage").style.display = 'none';
                    document.getElementById("copyNodesImage").style.display = 'none';
                    document.getElementById("moveNodesImage").style.display = 'none';                    
                }
            });
        });
});