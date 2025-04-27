(function($){
  $(function(){
	 
	$('.sidenav').sidenav();
	$('select').formSelect();
	$('.collapsible').collapsible();

	updateTooltips();
	updateCartCount();

	// Remove nsfw blur if disabled by user
	if (localStorage.hasOwnProperty("disabledNsfwCheck"))
		$(".game-blur").remove();

    currentlyPlayingMidi = "";
    
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
   
   $('.pagination-survey').pagination({
        items: $("#total_items").html(),
        itemsOnPage: 50,
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

	if (window.location.hash === "#iframe") {
		toggleiFrameMode();
	}
    
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

function truckClicked() {

	// Play ogg file
	var audio = new Audio('audio/cheering.ogg');
	audio.play();

	// One random message out of 9
	var r_text = new Array();
	r_text[0] = "Keep on trucking!";
	r_text[1] = "You can use '%' at the start of a search query to find more results...but it'll be slower!";
	r_text[2] = "Have you read the FAQ recently?";
	r_text[3] = "You can play a game by clicking on its image...but did you know you can play entire collections?";
	r_text[4] = "I'm a truck lol";
	r_text[5] = "Check the Upload page if you want to know how to extract .mio files from your saves.";
	r_text[6] = "bazinga";
	r_text[7] = "wow this is just like the news channel cat except cheaper";
	r_text[8] = "Stuff sent through DIY Showcase with WiiLink is automatically approved!";
	r_text[9] = 'The truck seems to have dropped...<a href="https://holopin.io/collect/clcv3bt7p577408l6cf44pbk6">something!</a>';
	var i = Math.floor(10 * Math.random())

	// Make sure the holopin message appears first if the user never saw it
    	if (localStorage.getItem("truckClicked") == null) {
		localStorage.setItem("truckClicked", "true");
		i = 9;
    	}
	
	popToast(r_text[i]);
}

function searchStore(category, name) {
	// url encode name
	name = encodeURIComponent(name);

	const href=`./${category}?name=${name}`;
	window.location.href = href;
}

function loadItems(pageNumber) {
	
	// Show a preloader
	$("#content").html('<center><div class="preloader-wrapper big active"><div class="spinner-layer"><div class="circle-clipper left">'+
	'<div class="circle"></div></div><div class="gap-patch"><div class="circle"></div></div><div class="circle-clipper right"><div class="circle"></div>'+
	'</div></div></div></center>');

	// Scroll up
	window.scrollTo(0,0);  

	// Strip "name" and "creator" as well as creator ID info from the URL's query params if they were set
	var url = window.location.href;
	url = url.replace(/name=[^&]*/g, "");
	url = url.replace(/creator=[^&]*/g, "");
	url = url.replace(/cartridge_id=[^&]*/g, "");
	url = url.replace(/creator_id=[^&]*/g, "");

	//Posts to itself -> one function for all three pages + surveys
	$.post( url, { page: pageNumber, 
									name: $("#item_name").val()?.trim(), 
									creator: $("#maker_name").val()?.trim(),
									cartridge_id: $("#cartridge_id")?.val(),
									creator_id: $("#creator_id")?.val(),
									sort_by: $("#sort_by")?.val()} )
		.done(function( data ) {		
			$("#content").html(data);
			updateTooltips();

			// Remove nsfw blur if disabled by user
			if (localStorage.hasOwnProperty("disabledNsfwCheck"))
				$(".game-blur").remove();

			$('.pagination').pagination('updateItems', $("#total_items").html());
			$('.pagination').pagination('drawPage', pageNumber);
			$('.pagination-survey').pagination('updateItems', $("#total_items").html());
			$('.pagination-survey').pagination('drawPage', pageNumber);
			$("#"+currentlyPlayingMidi+"-record").addClass("playing");
		})
		.fail(function() {
			popToast("Couldn't load items from DoujinSoft.");
			return 0;
		});

}

function loadCreatorInfo() {
	//TODO: Load creator info ONCE instead of repeated DB calls

	// // Show a preloader
	// $("#creatordetails").html('<center><div class="preloader-wrapper big active"><div class="spinner-layer"><div class="circle-clipper left">'+
	// '<div class="circle"></div></div><div class="gap-patch"><div class="circle"></div></div><div class="circle-clipper right"><div class="circle"></div>'+
	// '</div></div></div></center>');
	// //Scroll up
	// window.scrollTo(0,0);  
	
	// // Strip "name" and "creator" as well as creator ID info from the URL's query params if they were set
	// var url = window.location.href;
	// url = url.replace(/name=[^&]*/g, "");
	// url = url.replace(/creator=[^&]*/g, "");
	// url = url.replace(/cartridge_id=[^&]*/g, "");
	// url = url.replace(/creator_id=[^&]*/g, "");

}

function updateTooltips() {
	// Don't use jQuery for tooltips, looks like that's broken
	var elems = document.querySelectorAll('.tooltipped');
	M.Tooltip.init(elems, { enterDelay: 50 });
}

function clearSearch() {
	
	$("#item_name").val("");
	$("#maker_name").val("");
	$("#cartridge_id").val("");
	$("#creator_id").val("");
	M.updateTextFields();
	loadItems(1);
}

function searchForUser(creatorName, cartridgeId, creatorId) {

	$("#item_name").val("");

	if (cartridgeId !== "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" && cartridgeId !== "00000000000000000000000000000000" && creatorId !== "FFFF0000FFFFFFFF") {
		// IDs look legit
		$("#maker_name").val("");
		$("#cartridge_id").val(cartridgeId);
		$("#creator_id").val(creatorId);
	}
	else {
		// IDs are invalid, so we'll do a regular name search
		$("#maker_name").val(creatorName);
		$("#cartridge_id").val("");
		$("#creator_id").val("");
	}
	
	// Unfuck tooltips
	$(".material-tooltip").attr("style","visibility:hidden");

	// Do search
	M.updateTextFields();
	loadItems(1);
	loadCreatorInfo();
}

function drawManga(page1, page2, page3, page4) {

	  M.Materialbox.init($('.materialboxed')[0], {onOpenEnd: function(){
	    $('.materialboxed')[0].src = $('#canvas_manga')[0].toDataURL();}});

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

      $('.materialboxed')[0].src = "";
      M.Materialbox.getInstance($('.materialboxed')[0]).open();


	  //Fix materialize's auto-generated styling
      $('.materialboxed')[0].style.width = "auto";
      $('.materialboxed')[0].style.left = "30%";
}

function popToast(message) {
	M.toast({html:message, displayLength: 4000, classes: 'rounded grey lighten-4 black-text'});
}


// _fetch function taken from timidity by Feross Aboukhadijeh
// https://github.com/feross/timidity
async function _fetch(url) {
	const opts = {
		mode: 'cors',
		credentials: 'same-origin'
	}
	const response = await window.fetch(url, opts)
	if (response.status !== 200) {
		popToast(`Could not load ${url}`);
		throw new Error(`Could not load ${url}`)
	};

	const arrayBuffer = await response.arrayBuffer()
	const buf = new Uint8Array(arrayBuffer)
	return buf
}

async function playMidi(id) {
	
	$("#toast-container .toast").remove();
	$(".playing").removeClass("playing");

	if (!window.musicSamples) {
		window.musicSamples = await _fetch('./soundfont/ram_slice.bin')
	}

	if (id != currentlyPlayingMidi) {
	
		currentlyPlayingMidi=id;
		popToast("Playing MIDI for id "+id);
		
		$("#"+id+"-record").addClass("playing");
		let mioData = await _fetch('/download?type=record&id='+id);

		window.wahdio.play_music(mioData, window.musicSamples);
	}
	else {
		window.wahdio.stop_music();
		currentlyPlayingMidi = "";
		popToast("Playback Stopped.");
	}
}

// Remove parts of a singleitem page so that it can be embedded in an iframe
// eg <iframe src="https://diy.tvc-16.science/games?id=a7f667db4362842bee783123cd235699#iframe" width="536" height="490"/>
function toggleiFrameMode() {

	$("#index-banner").remove();
	$(".navbar-fixed").remove();
	$(".page-footer").remove();
	$("#iframe-info").remove();
	$(".container").width("100%");
	$("#total_items").html("<center style='color:white'>WarioWare DIY Embed powered by the <a href='http://diy.tvc-16.science' target='_parent'>DoujinSoft Store</a>.</center>");
	$("#total_items").show();
	$(".cart-btn").hide();
	$(".iframe-btn").show();
}

function copyShareLink(type, id) {

	str = "https://"+window.location.hostname+"/"+type+"?id="+id;

	navigator.clipboard.writeText(str)
	.then(() => {
		popToast("Link copied to your clipboard!");
	})
	.catch(() => {
		popToast("Couldn't copy link to your clipboard.");
	});
}

function addToCart(type, id) {

	var name = $("#"+id+"-name").html();
	var item = {id:id, name:name}; 
	
	if (localStorage.getItem(type) === null) {
		var items = [item];
		localStorage.setItem(type, JSON.stringify(items));
		popToast("Item added to cart successfully.");
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
			popToast("Item added to cart successfully.");
			}
		else
			popToast("Item already in cart.");
		
	}
	updateCartCount();
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
		
	updateCartCount();
}

function updateCartCount() {

	var count = 0;

	["game", "manga", "record"].forEach(function(entry) {
		if (localStorage.getItem(entry) !== null) 
		count += JSON.parse(localStorage.getItem(entry)).length;
	});

	$(".cart-header").each(function(){ this.innerHTML = "Cart (<b>"+count+"</b>)"});
}

function clearCart() {
    localStorage.removeItem("game");
    localStorage.removeItem("manga");
    localStorage.removeItem("record");
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
			localStorage.userWiiCode = code;
			$("#recipient").val(localStorage.userWiiCode);
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

            // Since this downloads a file it's easier to just submit the form as-is
            $("#checkout-form").submit();
            clearCart();
            $("#checkout-confirm").html('<div class="progress"><div class="indeterminate amber"></div></div>Thanks for using DoujinSoft! Your save file will automatically download once done.');
        }
        else
            popToast("Please upload a savefile before checking out your content.");
    }

    if ($("#wc24-radio")[0].checked) {

		code = $("#recipient").val();
        if (code.length == 16 && /^\d+$/.test(code)) {

		   // Save user's code to localStorage
		   localStorage.userWiiCode = $("#recipient").val();

           $("#checkout-confirm").html('<div class="progress"><div class="indeterminate amber"></div></div>Sending data over...');
           // POST the form to display progress
           var formData = new FormData($("#checkout-form")[0]);
           $.ajax({
               type: "POST",
               url: "./cart",
               data: formData,
               processData: false,
               contentType: false,
               success: function(data) {
				console.log(data);
				if (data.startsWith("cd=100")) {
					$("#checkout-confirm").html('<div class="progress"><div class="determinate green" style="width: 100%"></div></div>Thanks for using DoujinSoft! Look out for the blue light on your Wii.');
                	clearCart();
				} else {
					$("#checkout-confirm").html('<div class="progress"><div class="determinate red" style="width: 100%"></div></div>'+
					'Something seems to have gone wrong! Here\'s an error message: </br> <pre>'+data+'</pre>');
				}
                
               }
           });

        }
        else
           popToast("Please enter a Wii Friend Code before checking out your content.");

    }

}
