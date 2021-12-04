# Project Title

Location Reminder

## Getting Started

A TODO list app with location reminders that remind the user to do something when the user is at a specific location. The app will require the user to create an account and login to set and access reminders.

### Installation

To get the project running on your local machine, you need to follow these steps:

**Step 1: Clone the repo**

Use this to clone it to your local machine:
```bash
git clone https://github.com/Raghav-Saboo/UdacityLocationReminder.git
```

**Step 2: Run the project and check that it compiles correctly**

Open the project in Android Studio and click the Run ‘app’ button, check that it runs correctly and you can see the app in your device or emulator.

## Testing

Explain the steps needed to run any automated tests

### Break Down Tests

Explain what each test does and why

```
Examples here
```
## Project Instructions

- Project Instructions
- Create a Login screen to ask users to login using an email address or a Google account. Upon successful login, navigate the user to the Reminders screen. If there is no account, the app should navigate to a Register screen.
- Create a Register screen to allow a user to register using an email address or a Google account.
- Create a screen that displays the reminders retrieved from local storage. If there are no reminders, display a "No Data" indicator. If there are any errors, display an error message.
- Create a screen that shows a map with the user's current location and asks the user to select a point of interest to create a reminder.
- Create a screen to add a reminder when a user reaches the selected location. Each reminder should include
    - title
    - description
    - selected location
- Reminder data should be saved to local storage.
- For each reminder, create a geofencing request in the background that fires up a notification when the user enters the geofencing area.
- Provide testing for the ViewModels, Coroutines and LiveData objects.
- Create a FakeDataSource to replace the Data Layer and test the app in isolation.
- Use Espresso and Mockito to test each screen of the app:
- Test DAO (Data Access Object) and Repository classes.
- Add testing for the error messages.
- Add End-To-End testing for the Fragments navigation.



