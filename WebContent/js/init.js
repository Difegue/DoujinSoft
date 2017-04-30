(function($){
  $(function(){
	 
    $('.button-collapse').sideNav();

    
    $('.pagination-sm').twbsPagination({
        totalPages: 35,
        visiblePages: 7,
        first: "|<",
        prev: "<",
        next:">",
        last: ">|",
        onPageClick: function (event, page) {
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
			
			//return data;
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