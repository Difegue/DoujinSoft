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
			alert("error wow");
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

	if (localStorage.getItem(type) === null) {
		var items = [id];
		localStorage.setItem(type, JSON.stringify(items));
		Materialize.toast("Item added to cart successfully.", 4000, 'rounded grey lighten-4 black-text');
	}
	else {
		var items = JSON.parse(localStorage.getItem(type));
		if (items.indexOf(id) === -1) {
			items.push(id);
			localStorage.setItem(type, JSON.stringify(items));
			Materialize.toast("Item added to cart successfully.", 4000, 'rounded grey lighten-4 black-text');
			}
		else
			Materialize.toast("Item already in cart.", 4000, 'rounded grey lighten-4 black-text');
		
	}
	
	
}