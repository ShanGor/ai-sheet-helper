import { setupToolbar } from './setup-toolbar'
import React, {useEffect, useState} from 'react'
import './style.css'
import {SetupUniver} from './setupUniver.jsx'
import {fetchEvents} from "./Utility.js";

const mapColumn = (column) => {
    const alphabets = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    if (column < alphabets.length) {
        return alphabets[column]
    } else {
        return mapColumn(Math.floor(column / alphabets.length) - 1) + alphabets[column % alphabets.length]
    }
}

function App() {
  // const univerAPI = setupUniver()
  // window.univerAPI = univerAPI
  // setupToolbar(univerAPI)
    const [ selections, setSelections ] = useState([])
    const [selection, setSelection] = useState({})
    const [processMode, setProcessMode] = useState('single')
    const [toolOutputColumn, setToolOutputColumn] = useState('')
    const [llmOutputColumn, setLlmOutputColumn] = useState('')
    const [chatText, setChatText] = useState('')
    const [univerAPI, setUniverAPI] = useState(null)

    useEffect(() => {
        if (selections.length > 0) {
            let {sheetId, startRow, endRow, startColumn, endColumn} = selections[0]
            setSelection(selections[0])
            console.log(selection);
            console.log(`Selections: ${sheetId}, ${mapColumn(startColumn)}${startRow+1}:${mapColumn(endColumn)}${endRow+1}`);
            if (sheetId) {
                setToolOutputColumn(mapColumn(startColumn + 1))
                setLlmOutputColumn(mapColumn(startColumn + 2))
            }
        } else {
            setSelection({})
        }
    }, [selections]);

    const getSelectionAsString = () => {
        if (selection.sheetId)
            return `${mapColumn(selection.startColumn)}${selection.startRow+1}:${mapColumn(selection.endColumn)}${selection.endRow+1}`

        return ''
    }

  const submitChatRequest = async () => {
      if (!chatText) {
          alert('Please enter a chat request')
          return
      }

      const fWorkbook = univerAPI.getActiveWorkbook();
      const fWorksheet = fWorkbook.getActiveSheet();

      for (let i = selection.startColumn; i <= selection.endColumn; i++) {
          for (let j = selection.startRow; j <= selection.endRow; j++) {
              const rowIdx = j;
              const colIdx = i;
              const range = `${mapColumn(colIdx)}${rowIdx+1}:${mapColumn(colIdx)}${rowIdx+1}`
              console.log('Processing cell', range)
              const fRange = fWorksheet.getRange(range);
              const cellText = fRange.getDisplayValue()
              const prompt = `${chatText}\n\n${cellText}`

              let targetToolRange = `${toolOutputColumn}${rowIdx+1}:${toolOutputColumn}${rowIdx+1}`
              let targetLlmRange = `${llmOutputColumn}${rowIdx+1}:${llmOutputColumn}${rowIdx+1}`
              const outToolRange = fWorksheet.getRange(targetToolRange);
              const outLlmRange = fWorksheet.getRange(targetLlmRange);
              let toolValue = ''
              let llmValue = ''
              console.log('Submitting chat request', prompt);
              await new Promise((resolve, reject) => {
                  fetchEvents('http://localhost:8080/api/sheet-chat', (sse) => {
                      const {id, event, data} = sse
                      if (event === 'tool') {
                          console.log('Received message', data);
                          toolValue += data
                          outToolRange.setValue(toolValue)
                      } else if (event === 'llm') {
                          llmValue += data
                          outLlmRange.setValue(llmValue)
                      } else if (event === 'done') {
                          resolve()
                      } else {
                          console.log('Received message', data);
                          resolve()
                      }
                  }, prompt)
              })

          }
      }


  }
  return (
      <div style={{width: '100vw', height: '100vh', display: 'flex'}}>
          <div style={{width: '70%'}}>
              <SetupUniver setSelections={setSelections} setUniverAPI={setUniverAPI}/>
          </div>

          <div style={{width: '30%'}} className={'toolbar-container'}>
              <h2>AI Sheet Helper</h2>
              <div>
                  <label>Current Selections: </label>
                  <span className='code'>
                      {selection.sheetId ? `${mapColumn(selection.startColumn)}${selection.startRow+1}:${mapColumn(selection.endColumn)}${selection.endRow+1}`: 'Not selected'}
                  </span>
              </div>
              <div>
                  <label>Process Mode: </label>
                  <select value={processMode} onChange={(e) => setProcessMode(e.target.value)}>
                      <option value="single">One cell at a time</option>
                      <option value="all">Group all cells together</option>
                  </select>
              </div>
              {processMode==='single' && selection.sheetId && <>
                  <div>
                      <label>Tool Output Column: </label>
                      <input value={toolOutputColumn} onChange={(e) => setToolOutputColumn(e.target.value)}/>
                  </div>
                  <div>
                      <label>AI Output Column: </label>
                      <input value={llmOutputColumn} onChange={(e) => setLlmOutputColumn(e.target.value)}/>
                  </div>
              </>
              }
              <hr style={{clear: 'both', display: 'block', height: '1px', color: 'gray', width: '98%'}}/>
              <div>
                  <label>AI Chat: </label>
                  <br/>
                  <textarea rows={10} style={{width: '98%'}}
                            value={chatText}
                            onChange={(e) => setChatText(e.target.value)}
                            placeholder='Ask AI to help you with your task'/>
                  <br/>
                  <button onClick={() => {submitChatRequest()}}>Submit</button>
              </div>
          </div>
      </div>
  )
}

export default App
