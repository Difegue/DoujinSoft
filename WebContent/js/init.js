(function($){
  $(function(){
	 
    $('.button-collapse').sideNav();

    
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


function loadItems(pageNumber)
{
	
	//showSpinner();
	
	//Posts to itself -> one function for all three pages
	$.post( window.location.href, { page: pageNumber, name: $("#item_name").val(), creator: $("#maker_name").val() } )
		.done(function( data ) {		
			$("#content").html(data);
			
			$('.pagination').pagination('updateItems', $("#total_items").html());
			$('.pagination').pagination('drawPage', pageNumber);
		})
		.fail(function() {
			alert("error wow");
			return 0;
		});

}



function clearSearch()
{
	$("#item_name").val("");
	$("#maker_name").val("");
	Materialize.updateTextFields();
	loadItems(1);
}

function searchForUser(creator)
{
	$("#item_name").val("");
	$("#maker_name").val(creator);
	Materialize.updateTextFields();
	loadItems(1);
	
}

function drawManga(page1, page2, page3, page4)
{
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