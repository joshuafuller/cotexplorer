<img src="https://github.com/user-attachments/assets/73e02706-e0ae-4279-aebb-ce7ba732b391" width=10% height=10%> 

# CoT Explorer

Cursor on Target (CoT) Explorer is a plugin that logs incoming and outgoing CoT messages, and provides some additional tools as outlined below. To use CoT Explorer, load the plugin from TAK Package Management.
<img src="https://github.com/user-attachments/assets/e8dbb1c8-a5fa-4204-9de2-8b89c3d661a8" width=50% height=50%>

Once the plugin is loaded, view the logged CoT messages by selecting the CoT Explorer icon in the ATAK Overflow menu:
<img src="https://github.com/user-attachments/assets/66f95758-4b08-4ff6-b414-8c87ade8e61a" width=50% height=50%> 

<img src="https://github.com/user-attachments/assets/85b71ccb-167b-4f5b-a576-0453f707bec6" width=50% height=50%> 

## Send

The Send button allows the user to input raw CoT XML and either plot it on the map or send it to contacts on the map.

<img src="https://github.com/user-attachments/assets/3f4e659b-8b01-4132-a94f-228e88389939" width=50% height=50%> 

### Plot

Selecting "Plot" will import the raw CoT XML and place the item on the map without sending.

<img src="https://github.com/user-attachments/assets/651d3f04-d37f-40d4-8fa5-1512c8ae0dd9" width=50% height=50%>  <img src="https://github.com/user-attachments/assets/5020b5a3-89f6-4a4d-bed0-5f46296b0d07" width=50% height=50%>

### Send...

Selecting "Send" will create the item on the map and then open the Contacts list to allow the user to send the item to other users, or broadcast to the entire TAK network.

<img src="https://github.com/user-attachments/assets/02525e61-47b6-40a0-8cf3-c68cc3b3da56" width=50% height=50%> 

## Save

The Save button will save a copy of the CoT Explorer log to the `/atak/tools/cotexplorer` folder on the device's internal storage. The file will be named `cotexplorer-YYYYMMDDTHHMMSSZ.txt` with a timestamp reflecting the time the log was saved.

<img src="https://github.com/user-attachments/assets/078cc4e5-6311-4df2-b006-077be91cd84f" width=50% height=50%> 

## Pause

The Pause button will pause logging of CoT events. Any CoT events received while the stream is paused will not be saved in the log or available for filtering.

## Clear

The Clear button will clear all logged CoT messages from the tool.

## Inspect

When the Inspect Button is selected, the user will be prompted to select an item on the map. When the user selects a CoT item on the map, a popup window will appear with the CoT XML message for that item. 

<img src="https://github.com/user-attachments/assets/b42f4344-6009-475e-b165-a83fd2670efb" width=50% height=50%> <img src="https://github.com/user-attachments/assets/bf5d97c8-d69e-4b04-b856-736fb6fb26a3" width=50% height=50%> 

Long pressing on the text will copy it to the device clipboard.

<img src="https://github.com/user-attachments/assets/84316b00-a12b-4b04-ba2f-4bda937419be" width=50% height=50%> 

## Filter

The Filter icon on the right side of the CoT Explorer pane allows the user to filter logged CoT messages for any string. CoT messages that match the string will be displayed in the CoT Explorer pane with the matching text highlighted. Messages that do not match will continue to be logged in the background. Clicking the Clear button will clear the filter and return to viewing all logged CoT messages.

<img src="https://github.com/user-attachments/assets/42ac3697-c1dd-486f-b880-307680c8356e" width=50% height=50%> <img src="https://github.com/user-attachments/assets/7d88821a-9a48-4e2a-8c43-dfe238622d7c" width=50% height=50%> 

## Known Issues

* The Inspect button does not handle stacked map items and will only display the CoT XML for the topmost item.
