(function($){
  $(function(){
	 
    $('.button-collapse').sideNav();

    idPlaying = "";
    
    $('.pagination').pagination({
        items: $("#total_items").html(),
        itemsOnPage: 9,
        currentPage: 1,
        displayedPages: 3,
        ellipsePageSet: false,
        cssStyle: '',
        prevText: '<i class="material-icons">chevron_left</i>',
        nextText: '<i class="material-icons">chevron_right</i>',
        onInit: function () {
            // fire first page loading
        },
        onPageClick: function (page, evt) {
        	loadItems(page);
        }
    });
    
    
  }); // end of document ready
})(jQuery); // end of jQuery name space

function escapeHtml (string) {
	
  entityMap = {
  		  '&': '&amp;',
  		  '<': '&lt;',
  		  '>': '&gt;',
  		  '"': '&quot;',
  		  "'": '&#39;',
  		  '/': '&#x2F;',
  		  '`': '&#x60;',
  		  '=': '&#x3D;'
  		};

  return String(string).replace(/[&<>"'`=\/]/g, function (s) {
    return entityMap[s];
  });
}

function loadItems(pageNumber) {
	
	//showSpinner();
	
	//Posts to itself -> one function for all three pages
	$.post( window.location.href, { page: pageNumber, name: $("#item_name").val(), creator: $("#maker_name").val() } )
		.done(function( data ) {		
			$("#content").html(data);
			
			$('.pagination').pagination('updateItems', $("#total_items").html());
			$('.pagination').pagination('drawPage', pageNumber);
			$('.tooltipped').tooltip({delay: 50});
			$("#"+idPlaying+"-record").addClass("playing");
		})
		.fail(function() {
			alert("Couldn't load items from DoujinSoft.");
			return 0;
		});

}



function clearSearch() {
	
	$("#item_name").val("");
	$("#maker_name").val("");
	Materialize.updateTextFields();
	loadItems(1);
}

function searchForUser(creator) {
	
	$("#item_name").val("");
	$("#maker_name").val(creator);
	Materialize.updateTextFields();
	loadItems(1);
	
}

function drawManga(page1, page2, page3, page4) {
	
	  ctx = $('#canvas_manga')[0].getContext('2d');
	  img1 = new Image();
	  img2 = new Image();
	  img3 = new Image();
	  img4 = new Image();
	
	  img1.src = page1;
	  img2.src = page2;
	  img3.src = page3;
	  img4.src = page4;
	  
	  //Draw base64 images on canvas with a 1px black border
	  img1.addEventListener('load', function() {
		  ctx.drawImage(img1, 1, 1);
		}, false);
	  
	  img2.addEventListener('load', function() {
		  ctx.drawImage(img2, 1, 129);
		}, false);
	  
	  img3.addEventListener('load', function() {
		  ctx.drawImage(img3, 1, 257);
		}, false);
	  
	  img4.addEventListener('load', function() {
		  ctx.drawImage(img4, 1, 385);
		}, false);
	   
	  $('.materialboxed').click();

}


function playMidi(id) {
	
	$("#toast-container .toast").remove();
	$(".playing").removeClass("playing");
	
	if (id != idPlaying) {
	
		//Set the function as message callback
		MIDIjs.message_callback = function(mes){
			if (!mes.includes("Loading instruments"))
				Materialize.toast(mes, 4000, 'rounded grey lighten-4 black-text');
		};
		
		idPlaying=id;
		
		$("#"+id+"-record").addClass("playing");
		MIDIjs.play('midi?id='+id);
	}
	else {
		MIDIjs.stop();
		idPlaying = "";
		Materialize.toast("Playback Stopped.", 4000, 'rounded grey lighten-4 black-text');
	}
}

function addToCart(type, id) {

	var name = $("#"+id+"-name").html();
	var item = {id:id, name:name}; 
	
	if (localStorage.getItem(type) === null) {
		var items = [item];
		localStorage.setItem(type, JSON.stringify(items));
		Materialize.toast("Item added to cart successfully.", 4000, 'rounded grey lighten-4 black-text');
	}
	else {
		var items = JSON.parse(localStorage.getItem(type));
		var itemExists = false;
		
		for (i=0; i< items.length; i++)
			if (items[i].id === id)
				itemExists = true;
		
		if (!itemExists) {
			items.push(item);
			localStorage.setItem(type, JSON.stringify(items));
			Materialize.toast("Item added to cart successfully.", 4000, 'rounded grey lighten-4 black-text');
			}
		else
			Materialize.toast("Item already in cart.", 4000, 'rounded grey lighten-4 black-text');
		
	}
	
}

function deleteFromCart(item) {
	
	id = item.attr('id');
	type = item.attr('type');
	
	console.log("deleting "+type+" with id "+id);
	
	//Open matching localStorage array, remove item and reinsert
	var items = JSON.parse(localStorage.getItem(type));
	itemPosition = null;
	
	for (i=0; i< items.length; i++) 
		if (items[i].id === id)
			itemPosition = i;
	
	items.splice(itemPosition,1);
	
	if (items.length > 0)
		localStorage.setItem(type, JSON.stringify(items));
	else
		localStorage.removeItem(type);
	
	item.remove();
	
}

function checkoutSave() {
	
	if ($("#filename").val() !== "") {
		//Fill form with localStorage if available
		
		if (localStorage.getItem("game") !== null)
			$("#cartg").val(localStorage.getItem("game"));
		
		if (localStorage.getItem("manga") !== null)
			$("#cartm").val(localStorage.getItem("manga"));
		
		if (localStorage.getItem("record") !== null)
			$("#cartr").val(localStorage.getItem("record"));
		
		
		$("#checkout-form").submit();
		
		//Empty localStorage and submit the form.
		localStorage.clear();
		$("#checkout-confirm").html('<div class="progress"><div class="indeterminate amber"></div></div>Thanks for using DoujinSoft! Your save file will automatically download once done.');
		
	}
	else
		alert("Please upload a savefile before checking out your content.")
	
}