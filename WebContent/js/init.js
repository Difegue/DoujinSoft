(function($){
  $(function(){
	 
    $('.sidenav').sidenav();

    idPlaying = "";
    
    $('.pagination').pagination({
        items: $("#total_items").html(),
        itemsOnPage: 15,
        currentPage: 1,
        displayedPages: 5,
        ellipsePageSet: true,
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
	
	games = JSON.parse(localStorage.getItem("game"));
	manga = JSON.parse(localStorage.getItem("manga"));
	record = JSON.parse(localStorage.getItem("record"));
	
	if (games === null && manga === null && record === null) 
		$("#cart-content").html('<h5 class="white-text center">...But your cart seems to be empty right now.</h5> <br/> <br/>  <span class="white-text" style="font-size:10vw">¯\\_(ツ)_/¯</span>   ');

}

function updateShipping(type) {

    $("#save-ship").hide();
    $("#wc24-ship").hide();

    if (type === "save")
        $("#save-ship").show();
    else
        $("#wc24-ship").show();
}

function registerFriendCode() {

    code = $("#friendreg").val();

    if (code.length == 16 && /^\d+$/.test(code)) {
        $("#regresult").html("Sending Friend Request...");

        $.get("./friendreq?code="+code, function(data, status){
            console.log(data);
            $("#regresult").html("✔ All set! You can now close this popup window.");
          });

    } else {
        $("#regresult").html("❌ Please enter a valid Friend Code!");
    }
}

function checkoutSave() {

	//Fill form with localStorage if available
    if (localStorage.getItem("game") !== null)
        $("#cartg").val(localStorage.getItem("game"));

    if (localStorage.getItem("manga") !== null)
        $("#cartm").val(localStorage.getItem("manga"));

    if (localStorage.getItem("record") !== null)
        $("#cartr").val(localStorage.getItem("record"));


    if ($("#save-radio")[0].checked) {

        if ($("#filename").val() !== "") {

            $("#checkout-form").submit();
            localStorage.clear();
            $("#checkout-confirm").html('<div class="progress"><div class="indeterminate amber"></div></div>Thanks for using DoujinSoft! Your save file will automatically download once done.');
        }
        else
            M.toast({html:"Please upload a savefile before checking out your content."});
    }

    if ($("#wc24-radio")[0].checked) {

        if ($("#recipient").val() !== "") {

           $("#checkout-form").submit();
           localStorage.clear();
           $("#checkout-confirm").html('<div class="progress"><div class="indeterminate amber"></div></div>Thanks for using DoujinSoft! Look out for the blue light on your Wii.');
        }
        else
           M.toast({html:"Please enter a Wii Friend Code before checking out your content."});

    }

}
