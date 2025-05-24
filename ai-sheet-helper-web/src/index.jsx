import React from 'react'
import ReactDOM from 'react-dom/client'

import {createBrowserRouter, RouterProvider} from "react-router";
import App from "./App.jsx";

let routes = [
    {
        path: "/",
        index: true,
        element: <App/>,
    },
]
ReactDOM.createRoot(document.getElementById('root')).render(
    // <React.StrictMode>
    <RouterProvider router={createBrowserRouter(routes)} />
    // </React.StrictMode>,
)
