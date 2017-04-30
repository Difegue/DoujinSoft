(function($){
  $(function(){
	 
    $('.button-collapse').sideNav();

    
    $('.pagination-sm').twbsPagination({
        totalPages: 35,
        visiblePages: 7,
        first: '<i class="material-icons">first_page</i>',
        prev: '<i class="material-icons">chevron_left</i>',
        next:'<i class="material-icons">chevron_right</i>',
        last: '<i class="material-icons">last_page</i>',
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

function resetPagination()
{
	
	$('.pagination-sm').twbsPagination('destroy');
	$('.pagination-sm').twbsPagination({
        totalPages: 35, //todo: use a number given by the server while treating gameDetail pages
        visiblePages: 7,
        first: '<i class="material-icons">first_page</i>',
        prev: '<i class="material-icons">chevron_left</i>',
        next:'<i class="material-icons">chevron_right</i>',
        last: '<i class="material-icons">last_page</i>',
        onPageClick: function (event, page) {
        	loadGames(page);
        }
    });

}

function clearSearch()
{
	$("#game_name").val("");
	$("#maker_name").val("");
	Materialize.updateTextFields();
	resetPagination();
}

function searchForUser(creator)
{
	$("#game_name").val("");
	$("#maker_name").val(creator);
	Materialize.updateTextFields();
	resetPagination();
	
}