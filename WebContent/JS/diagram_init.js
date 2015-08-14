var G = go.GraphObject.make;
var myDiagram;
var myModel;
var myLayout;

/*
 * Node Types
 */
var nodeTypeStartNode = "StartNode";
var nodeTypeEndNode = "EndNode";
var nodeTypeHumanTaskNode = "HumanTaskNode";
var nodeTypeActionNode = "ActionNode";
var nodeTypeCondition = "Gateway";
var nodeTypeEventNode = "EventNode";
var nodeTypeSplitAnd = "SPLIT AND";
var nodeTypeJoinAnd = "JOIN AND";


/*
* Node Names
*/
var nodeNameAND = "AND";
var nodeNameXOR = "XOR";
var nodeNameStart = "Start";
var nodeName = "End";
var nodeNameSignal = "Signal"

/*
* Node Colors
*/
var startNodeColor = "MediumSpringGreen";
var endNodeColor = "#FF0000";
var humanTaskNodeColor = "Silver";
var conditionNodeColor = "#F2C679";
var actionNodeColor = "Gainsboro";
var defaultNodeColor = "Silver";
var completedNodeColor = "lightgreen";
var selectedNodeColor = "lightblue"
var inProgressNodeColor = "lightgreen";
var subProcessStartNodeColor = "#BFFF9F";
var subProcessEndNodeColor = "LightCoral";
var nodeBorderColor = "grey";
var nodeBorderColorHighlighted = "white";
var frontierNodeColor = "Ivory";

/*
* Node size info
*/
var startNodeWidth = 20;
var endNodeWidth = 16;
var humanTaskNodeWidth = 60;
var conditionNodeWidth = 10;
var defaultNodeWidth = 80;
var xorNodeWidth = 50;
var eventNodeWidth =100;
var defaultNodeStrokeWidth = 2;
var endNodeStrokeWidth = 4;

var startNodeHeight = 20;
var endNodeHeight = startNodeHeight - ((endNodeStrokeWidth - defaultNodeStrokeWidth)*2);
var humanTaskNodeHeight = 40;
var conditionNodeHeight = 60;
var defaultNodeHeight = 40;
var xorNodeHeight = 50;
var eventNodeHeight = 40;

function init(){
 initDiagram();
 initNodeTemplate();
 initNodeGroups();
 initModel();
 initLayout();
 initLinkTemplate();
 generateTableData();
 setProcessState();
 setAllNodeColors();
 initLinkColors()
}

/*
*Grabs the "myDiagram" div from the html doc and initializes a new diagram
*/
function initDiagram(){
 myDiagram =
   G(go.Diagram, "myDiagramDiv",
     {
       initialContentAlignment:go.Spot.Center,
       /*
        * Diagram Permissions
        * set diagram to read only, to prevent unwanted user ineraction
        * Diagram Panning and mousewheel operations are also disabled on the diagram
        * set maximum number of selected elements to 1
        */
       isReadOnly: true,
       allowHorizontalScroll: true,
       allowVerticalScroll: true,
       allowZoom: true,
       allowSelect: false,
       maxSelectionCount: 1,
       click: selectBackground,
     });
}

/*
* initialise the node template for the diagram
*/
function initNodeTemplate(){
 myDiagram.nodeTemplate =
   G(go.Node, "Auto",
     {
       mouseEnter: highlightNode,
       mouseLeave: unhighlightNode,
       click: selectItem,
       selectable: false,
       avoidableMargin: G(go.Margin, {
         bottom: 0,
         left: 0,
         right: 0,
         top: 0,
       }),
     },
     G(go.Shape, {
           name: "SHAPE",
           //strokeWidth: 2,
           stroke: nodeBorderColor,
           //though the inital value is null, this will be set to another color at a later point
           //in the diagram set up
           fill: null,
         },
         /*
          * The shape of a node is dependant on its type
          * start and end nodes are circles
          * task nodes are squares
         */
         new go.Binding("figure", "nodeType", function(nodeType){
             if(nodeType == nodeTypeStartNode || nodeType == nodeTypeEndNode) return "Circle";
             else if(nodeType == nodeTypeHumanTaskNode) return "Rectangle"
             else if (nodeType == nodeTypeCondition) return "Rectangle";
             else if (nodeType == nodeTypeEventNode) return "SquareArrow";
             else return "Rectangle";
           }),
         new  go.Binding("figure", "name", function(name){
             if(name == nodeNameXOR) return "Diamond";
         }),
         /*
          * The size of a node is also dependant on its type
          * end and start nodes 20
          * task nodes 40
          */
         new go.Binding("width", "nodeType", function(nodeType){
             if(nodeType == nodeTypeStartNode) return startNodeWidth;
             else if(nodeType == nodeTypeEndNode) return endNodeWidth;
             else if(nodeType == nodeTypeHumanTaskNode) return humanTaskNodeWidth;
             else if (nodeType == nodeTypeCondition) return conditionNodeWidth;
             else if (nodeType == nodeTypeEventNode) return eventNodeWidth;
             else return defaultNodeWidth;
           }),
         new  go.Binding("width", "name", function(name){
             if(name == nodeNameXOR) return xorNodeWidth;
         }),
         new go.Binding("height", "nodeType", function(nodeType){
             if(nodeType == nodeTypeStartNode) return startNodeHeight;
             else if(nodeType == nodeTypeEndNode) return endNodeHeight;
             else if(nodeType == nodeTypeHumanTaskNode) return humanTaskNodeHeight;
             else if (nodeType == nodeTypeCondition) return conditionNodeHeight;
             else if (nodeType == nodeTypeEventNode) return eventNodeHeight;
             else return defaultNodeHeight;
           }),
         new  go.Binding("height", "name", function(name){
             if(name == nodeNameXOR) return xorNodeHeight;
         }),

         /*
          * Stroke Width of end nodes is to be heavier
          */
         new go.Binding("strokeWidth", "nodeType", function(nodeType){
           if(nodeType == nodeTypeEndNode) return endNodeStrokeWidth;
           else return defaultNodeStrokeWidth;
         })
     ),
     /*
      * Adding text to the nodes
      */
     G(go.TextBlock,
       {
         font: "bold 8pt Helvetica, Arial, sans-serif",
         stroke: "#555555",
       },
       /*
        * Our text is only visible in non end and non start blocks
        * Conditional Nodes of AND variety also have no label
        */
       new go.Binding("text", "name", function(name){
         if(!name.includes(nodeNameAND)) {
           if(name.length > 10){
             return name.substring(0, 9) + "...";
           }
           else {
             return name;
           }
         }
       }),
       new go.Binding("text", "nodeType", function(nodeType){
         if(nodeType === nodeTypeStartNode || nodeType === nodeTypeEndNode){
           return null;
         }
         return text;
       })
     )
   );
}

//initializes the nodes colors in the diagram
function setAllNodeColors(){
 var nodeData = myDiagram.model.nodeDataArray;
 for(var i = 0; i < nodeData.length; i ++){
   var nodeInDiagram = myDiagram.findNodeForData(nodeData[i]);
   if(myDiagram.findNodeForData(nodeData[i]) === currentSelectedItem) setItemColor(nodeInDiagram, true);
   else setItemColor(nodeInDiagram, false);
 }
}

/*
* Initialises the node groups for the diagram
*/
function initNodeGroups(){
 myDiagram.groupTemplate =
   G(go.Group, "Vertical", {
       //mouseEnter: highlightNode,
       //mouseLeave: unhighlightNode,
       //isSubGraphExpanded: false,
       click: selectItem,
       layout: G(go.TreeLayout, {
             nodeSpacing: 25,
             layerSpacing: 100,
           }
         ),
     },
     G(go.Panel, "Auto",
       G(go.Shape, "RoundedRectangle",  // surrounds the Placeholder
         {
           name: "SHAPE",
           stroke: nodeBorderColor,
           fill: "rgba(128,128,128,0.33)",
         }),
        G(go.Placeholder,    // represents the area of all member parts,
          { padding: 25 }),  // with some extra padding around them
       G(go.TextBlock,         // group title
         { alignment: go.Spot.Top, font: "Bold 12pt Sans-Serif", margin: 5, },
         new go.Binding("text", "name"))
     )
   );
}

/*
* Initializes the diagrams data model
*/
function initModel(){
 myModel = G(go.GraphLinksModel);
 myModel.nodeDataArray = currentProcess.nodeData;
 myModel.linkDataArray = currentProcess.connections;
 myDiagram.model = myModel;
}

/*
* Initializes the layout for the nodes
*/
function initLayout(){
 myLayout = G(go.TreeLayout, {
     nodeSpacing: 25,
     layerSpacing: 100,
   });
 myDiagram.layout = myLayout;
}

/*
* Initializes the diagram with a template for the node links
*/
function initLinkTemplate(){
 myDiagram.linkTemplate =
   G(go.Link,
     { routing: go.Link.AvoidsNodes, corner: 2, selectable: false , click: selectItem},
     G(go.Shape, { name: "SHAPE", strokeWidth: 2, stroke: "grey", }),
     G(go.Shape, { toArrow: "standard", stroke: "grey", fill: "grey" }),
     G(go.TextBlock,
       {
         name: "TEXT",
         font: "bold 8pt sans-serif",
         stroke: "black",
         editable: false,
       },
       new go.Binding("text", "condition", function(condition){
         if(condition != undefined){
           return "condition";
         }
     })
   )
 );
}

//initializes the link colors in the diagram
function initLinkColors(){
var linkData = myDiagram.model.linkDataArray;
for(var i = 0; i < linkData.length; i ++){
 var linkInDiagram = myDiagram.findLinkForData(linkData[i]);
 setItemColor(linkInDiagram, false);
}
}

/*
 * function to change color of node
 */
function setItemColor(item, selected){
var shape = item.findObject("SHAPE");
var text = item.findObject("TEXT");
var itemType = item.data.nodeType;

if(shape !== null){
 //if selectedItem is a node
 if(item.data.hasOwnProperty("nodeType")){
   var itemType = item.data.nodeType;
   if(itemType == nodeTypeStartNode && item.data.group !== undefined) shape.fill = subProcessStartNodeColor;
   else if(itemType == nodeTypeEndNode && item.data.group !== undefined) shape.fill = subProcessEndNodeColor;
   else if(itemType == nodeTypeStartNode) shape.fill = startNodeColor;
   else if(itemType == nodeTypeEndNode) shape.fill = endNodeColor;
   else if(itemType == nodeTypeHumanTaskNode) shape.fill = humanTaskNodeColor;
   else if(itemType == nodeTypeCondition) shape.fill = conditionNodeColor;
   else if(itemType == nodeTypeActionNode) shape.fill = actionNodeColor;
   else if (itemType == nodeTypeEventNode) shape.fill = conditionNodeColor;
   else shape.fill = defaultNodeColor;
 }

 else if(item.data.hasOwnProperty("condition")){
   text.stroke = "#3F5F5F";
   shape.stroke = "#C3C3C3";
 }

 if (item.data.completed === true) shape.fill = completedNodeColor;
 if(inProgressNode !== undefined){
   if(item.data.key === inProgressNode.key) shape.fill = inProgressNodeColor;
   if(nodeFrontier.indexOf(item.data.key) !== -1) shape.fill = frontierNodeColor;
 }
 if(selected && item.data.hasOwnProperty("nodeType")) shape.fill = selectedNodeColor;
 else if(selected && item.data.hasOwnProperty("condition")) {
   text.stroke = "black";
   shape.stroke = selectedNodeColor;
 }
}
}

//used for testing
function getNodeByKey(num){
  for(var i = 0; i < currentProcess.nodeData.length; i++ ){
    if(currentProcess.nodeData[i].key === num){
      return currentProcess.nodeData[i];
    }
  }
}

//returns node with input name
function getNodeByName(name){
  for(var i = 0; i < currentProcess.nodeData.length; i++ ){
    if(currentProcess.nodeData[i].name === name){
      return currentProcess.nodeData[i];
    }
  }
}

function setProcessState(){
  var node = getNodeByName(processStateData.taskdescriptions.taskdescription.name);
  for(var i = 0; i < myDiagram.model.nodeDataArray.length; i ++){
    if(myDiagram.model.nodeDataArray[i].name === node.name){
      myDiagram.model.nodeDataArray[i].completed = true;
    }
  }
}
