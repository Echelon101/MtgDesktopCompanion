
function formatMana(manaString)
{
	if(manaString.includes("/P"))
	{
		return manaString.replace(/\//g, '');
	}
	else if (manaString.includes("/"))
	{
		var s = manaString.replace(/\//g, '');
		s += " ms-split ";
		return s;
	}
	return manaString;
}



function mtgtooltip(element)
{
	element.popover({
        placement : 'top',
		trigger : 'hover',
        html : true,
        content: function () {
            var set = $(this).attr("data-set");
            var name=$(this).attr("data-name");
            var uri = '<img src="'+restserver+"/pics/cards/"+set+"/"+name+'">';
            
            if(set===undefined)
            	{
            	uri = '<img src="'+restserver+"/pics/cardname/"+name+'">';
            	}
            
            console.log(uri);
            return uri;
        }
    });
	
}


function mtgtooltipStock(element)
{
	element.popover({
        placement : 'top',
		trigger : 'hover',
        html : true,
        content: function () {
            return '<img width="250px" src="'+$(this).attr("productUrl")+'"/>';
        }
    });
	
}