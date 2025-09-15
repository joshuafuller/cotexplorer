# CoT Explorer

 Cursor on Target (CoT) Explorer is a plugin that logs incoming and outgoing CoT messages, and provides some additional tools as outlined below.

## Send

The Send button allows the user to input raw CoT XML and either plot it on the map or send it to contacts on the map.

### Plot

Selecting "Plot" will import the raw CoT XML and place the item on the map without sending.

### Send...

Selecting "Send" will create the item on the map and then open the Contacts list to allow the user to send the item to other users, or broadcast to the entire TAK network.

## Save

The Save button will save a copy of the CoT Explorer log to the `/atak/tools/cotexplorer` folder on the device's internal storage. The file will be named `cotexplorer-YYYYMMDDTHHMMSSZ.txt` with a timestamp reflecting the time the log was saved.

## Pause

The Pause button will pause logging of CoT events. Any CoT events received while the stream is paused will not be saved in the log or available for filtering.

## Clear

The Clear button will clear all logged CoT messages from the tool.

## Inspect

When the Inspect Button is selected, the user will be prompted to select an item on the map. When the user selects a CoT item on the map, a popup window will appear with the CoT XML message for that item. The user can long press on the text to copy it to their device clipboard.

## Filter

The Filter icon on the right side of the CoT Explorer pane allows the user to filter logged CoT messages for any string. CoT messages that match the string will be displayed in the CoT Explorer pane with the matching text highlighted. Messages that do not match will continue to be logged in the background. Clicking the Clear button will clear the filter and return to viewing all logged CoT messages.

## Known Issues

* The Inspect button does not handle stacked map items and will only display the CoT XML for the topmost item.

