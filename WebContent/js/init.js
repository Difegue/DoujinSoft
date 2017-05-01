(function($){
  $(function(){
	 
    $('.button-collapse').sideNav();

    
    $('.pagination').pagination({
        items: $("#total_games").html(),
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
        	loadGames(page);
        }
    });
    
  }); // end of document ready
})(jQuery); // end of jQuery name space


function loadGames(pageNumber)
{
	
	//showSpinner();
	
	$.get( "games", { page: pageNumber, name: $("#game_name").val(), creator: $("#maker_name").val() } )
		.done(function( data ) {		
			$("#content").html(data);
			
			$('.pagination').pagination('updateItems', $("#total_games").html());
			$('.pagination').pagination('drawPage', pageNumber);
		})
		.fail(function() {
			alert("error wow");
			return 0;
		});

}



function clearSearch()
{
	$("#game_name").val("");
	$("#maker_name").val("");
	Materialize.updateTextFields();
	loadGames(1);
}

function searchForUser(creator)
{
	$("#game_name").val("");
	$("#maker_name").val(creator);
	Materialize.updateTextFields();
	loadGames(1);
	
}