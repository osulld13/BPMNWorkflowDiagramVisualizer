/*
 * Items Being Used
 */
var currentProcess = processes[0];
var currentSelectedItem;

/*
 * Highlights a node passed to the function when called
 */
function highlightNode(e, node) {
  var shape = node.findObject("SHAPE");
  shape.stroke = nodeBorderColorHighlighted;
}

/*
 * Unhighlights a node passed to the function when called
 */
function unhighlightNode(e, node) {
  var shape = node.findObject("SHAPE");
  shape.stroke = nodeBorderColor;
}

/*
 * function to select the diagrams background
 * this will result in no node being selected
 */
 function selectBackground(e){
   if (currentSelectedItem !== undefined){
     setItemColor(currentSelectedItem, false);
   }
   currentSelectedItem = undefined;
   generateTableData();
 }

 /*
  * function for selecting a node and performing associated operations
  */
 function selectItem(e, item){
   if(item.data.hasOwnProperty("nodeType") || item.data.hasOwnProperty("condition")){
     if (item !== null || item !== currentSelectedItem) {
       if(currentSelectedItem !== undefined){
         setItemColor(currentSelectedItem, false);
       }
       setItemColor(item, true);
       currentSelectedItem = item;
       generateTableData();
     }
   }
   //move to node if is in the frontier
   if(nodeFrontier.indexOf(item.data.key) !== -1){
     advanceToNode(getNodeByKey(item.data.key));
   }
 }
