import { createUniver, defaultTheme, LocaleType, merge } from '@univerjs/presets'
import { UniverSheetsCorePreset } from '@univerjs/presets/preset-sheets-core'
import UniverPresetSheetsCoreEnUS from '@univerjs/presets/preset-sheets-core/locales/en-US'
import React, { useEffect } from 'react'

import '@univerjs/presets/lib/styles/preset-sheets-core.css'

export const SetupUniver = ({setSelections, setUniverAPI}) => {
  useEffect(() => {
    const { univerAPI } = createUniver({
      locale: LocaleType.EN_US,
      locales: {
        [LocaleType.EN_US]: merge(
          {},
          UniverPresetSheetsCoreEnUS,
        ),
      },
      theme: defaultTheme,
      presets: [
        UniverSheetsCorePreset({
          container: 'univer-container',
        }),
      ],
    })

    univerAPI.createWorkbook({ name: 'Test Sheet' })
    univerAPI.addEvent(univerAPI.Event.SelectionChanged, (params) => {
      const { worksheet, workbook, selections } = params;
      setSelections(selections)
    });

    setUniverAPI(univerAPI)

    return () => {
      univerAPI.dispose()
    }
  }, [])

  return (
    <div id="univer-container" style={{ width: '100%', height: '100%' }}></div>
  )
}
